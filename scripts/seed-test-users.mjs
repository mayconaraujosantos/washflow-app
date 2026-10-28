import { randomBytes } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { createInterface } from "node:readline";

const apiBaseUrl = "http://127.0.0.1:7000";
const managerPhone = "11999993333";
const credentialsPath = ".local-test-users";

function readDotenvValue(key) {
  if (!existsSync(".env")) return undefined;

  const line = readFileSync(".env", "utf8")
    .split(/\r?\n/)
    .find((entry) => entry.trimStart().startsWith(`${key}=`));
  return line
    ?.slice(line.indexOf("=") + 1)
    .trim()
    .replace(/^['"]|['"]$/g, "");
}

function assertDevelopmentEnvironment() {
  const environment =
    process.env.APP_ENV ?? readDotenvValue("APP_ENV") ?? "development";
  if (environment.toLowerCase() === "production") {
    throw new Error("Refusing to create test users when APP_ENV=production.");
  }
}

function generatePhone() {
  const suffix = String(randomBytes(4).readUInt32BE() % 1_000_000).padStart(
    6,
    "0",
  );
  return `00000${suffix}`;
}

function createCredentials() {
  return {
    customer: {
      name: "Cliente Teste Local",
      phone: generatePhone(),
      password: randomBytes(24).toString("base64url"),
    },
    washer: {
      name: "Lavador Teste Local",
      phone: generatePhone(),
      password: randomBytes(24).toString("base64url"),
    },
  };
}

function loadOrCreateCredentials() {
  if (existsSync(credentialsPath)) {
    const contents = readFileSync(credentialsPath, "utf8");
    const values = Object.fromEntries(
      contents
        .split(/\r?\n/)
        .filter((line) => line && !line.startsWith("#"))
        .map((line) => {
          const separator = line.indexOf("=");
          return [line.slice(0, separator), line.slice(separator + 1)];
        }),
    );
    if (
      !values.TEST_CUSTOMER_PHONE ||
      !values.TEST_CUSTOMER_PASSWORD ||
      !values.TEST_WASHER_PHONE ||
      !values.TEST_WASHER_PASSWORD
    ) {
      throw new Error(
        `${credentialsPath} exists but is missing test account values.`,
      );
    }
    return {
      customer: {
        name: "Cliente Teste Local",
        phone: values.TEST_CUSTOMER_PHONE,
        password: values.TEST_CUSTOMER_PASSWORD,
      },
      washer: {
        name: "Lavador Teste Local",
        phone: values.TEST_WASHER_PHONE,
        password: values.TEST_WASHER_PASSWORD,
      },
    };
  }

  const credentials = createCredentials();
  const contents = [
    "# Local development test users. Do not commit or use these outside development.",
    `TEST_CUSTOMER_PHONE=${credentials.customer.phone}`,
    `TEST_CUSTOMER_PASSWORD=${credentials.customer.password}`,
    `TEST_WASHER_PHONE=${credentials.washer.phone}`,
    `TEST_WASHER_PASSWORD=${credentials.washer.password}`,
    "",
  ].join("\n");
  writeFileSync(credentialsPath, contents, { mode: 0o600, flag: "wx" });
  return credentials;
}

async function readHidden(prompt) {
  if (!process.stdin.isTTY || typeof process.stdin.setRawMode !== "function") {
    throw new Error(
      "Run this command in an interactive terminal to enter the manager password.",
    );
  }

  process.stdout.write(prompt);
  process.stdin.setEncoding("utf8");
  process.stdin.setRawMode(true);
  process.stdin.resume();

  return new Promise((resolve, reject) => {
    let value = "";
    const cleanup = () => {
      process.stdin.off("data", onData);
      process.stdin.setRawMode(false);
      process.stdin.pause();
      process.stdout.write("\n");
    };
    const onData = (chunk) => {
      for (const character of chunk) {
        if (character === "\u0003") {
          cleanup();
          reject(new Error("Cancelled."));
          return;
        }
        if (character === "\r" || character === "\n") {
          cleanup();
          resolve(value);
          return;
        }
        if (character === "\u007f" || character === "\b") {
          value = value.slice(0, -1);
        } else {
          value += character;
        }
      }
    };

    process.stdin.on("data", onData);
  });
}

async function request(path, { method = "GET", body, token } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${apiBaseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const responseBody = await response.json().catch(() => null);
  if (!response.ok) {
    const message =
      responseBody?.error ?? `HTTP ${response.status} from ${path}`;
    const error = new Error(message);
    error.status = response.status;
    error.endpoint = path;
    throw error;
  }
  return responseBody;
}

async function authenticateManager(password) {
  return request("/api/auth/managers", {
    method: "POST",
    body: { phone: managerPhone, password },
  });
}

async function authenticateCustomer(account) {
  return request("/api/auth/customers", {
    method: "POST",
    body: {
      name: account.name,
      phone: account.phone,
      password: account.password,
    },
  });
}

async function provisionWasher(account, managerSession) {
  try {
    const candidate = await authenticateCustomer(account);
    await request(`/api/users/${candidate.user.id}/profile`, {
      method: "PATCH",
      token: managerSession.token,
      body: {
        managerId: managerSession.user.id,
        newProfile: "WASHER",
      },
    });
  } catch (error) {
    if (error.status !== 409) throw error;
    await request("/api/auth/washers", {
      method: "POST",
      body: { phone: account.phone, password: account.password },
    });
  }
}

async function main() {
  assertDevelopmentEnvironment();
  let currentStep = "checking the local API";
  try {
    await request("/api/health");

    const managerPassword = await readHidden(
      `Manager password for ${managerPhone} (not displayed): `,
    );
    if (managerPassword.length < 8) {
      throw new Error(
        "Manager password must contain at least eight characters.",
      );
    }

    const credentials = loadOrCreateCredentials();
    currentStep = "authenticating the seeded manager";
    const managerSession = await authenticateManager(managerPassword);
    currentStep = "creating the local customer account";
    await authenticateCustomer(credentials.customer);
    currentStep = "creating and promoting the local washer account";
    await provisionWasher(credentials.washer, managerSession);
    currentStep = "verifying the washer account";
    await request("/api/auth/washers", {
      method: "POST",
      body: {
        phone: credentials.washer.phone,
        password: credentials.washer.password,
      },
    });

    console.log("Local test accounts are ready:");
    console.log(`Manager: ${managerPhone} (use the password you entered)`);
    console.log(
      `Customer: ${credentials.customer.phone} / ${credentials.customer.password}`,
    );
    console.log(
      `Washer: ${credentials.washer.phone} / ${credentials.washer.password}`,
    );
    console.log(
      `Customer and washer credentials are saved in ${credentialsPath}.`,
    );
    console.log(
      "Sign into each role in a separate browser session to test the full flow.",
    );
  } catch (error) {
    const status = error.status
      ? ` (HTTP ${error.status}${error.endpoint ? ` ${error.endpoint}` : ""})`
      : "";
    console.error(
      `Could not provision local test users while ${currentStep}${status}: ${error.message}`,
    );
    if (error.status >= 500) {
      console.error(
        "Check the backend terminal for the logged exception and restart it if its code is stale.",
      );
    }
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(`Could not provision local test users: ${error.message}`);
  process.exitCode = 1;
});
