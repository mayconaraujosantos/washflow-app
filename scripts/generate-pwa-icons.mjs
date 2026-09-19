import { mkdir, writeFile } from 'node:fs/promises'
import { deflateSync } from 'node:zlib'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outputDir = path.join(root, 'web', 'public', 'icons')

function crc32(buffer) {
  let crc = 0xffffffff
  for (const byte of buffer) {
    crc ^= byte
    for (let bit = 0; bit < 8; bit += 1) {
      crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0)
    }
  }
  return (crc ^ 0xffffffff) >>> 0
}

function chunk(type, data) {
  const typeBuffer = Buffer.from(type)
  const length = Buffer.alloc(4)
  length.writeUInt32BE(data.length)
  const checksum = Buffer.alloc(4)
  checksum.writeUInt32BE(crc32(Buffer.concat([typeBuffer, data])))
  return Buffer.concat([length, typeBuffer, data, checksum])
}

function createPng(size) {
  const pixels = Buffer.alloc((size * 4 + 1) * size)
  const center = size / 2
  const radius = size * 0.22
  const scale = size / 512
  const lightning = [
    [278, 54], [92, 282], [218, 282], [218, 458], [420, 208], [284, 208]
  ]

  function insideLightning(x, y) {
    let inside = false
    for (let index = 0, previous = lightning.length - 1; index < lightning.length; previous = index++) {
      const [x1, y1] = lightning[index]
      const [x2, y2] = lightning[previous]
      if ((y1 > y) !== (y2 > y) && x < ((x2 - x1) * (y - y1)) / (y2 - y1) + x1) inside = !inside
    }
    return inside
  }

  for (let y = 0; y < size; y += 1) {
    const row = y * (size * 4 + 1)
    pixels[row] = 0
    for (let x = 0; x < size; x += 1) {
      const distanceX = Math.max(Math.abs(x - center) - center + radius, 0)
      const distanceY = Math.max(Math.abs(y - center) - center + radius, 0)
      const rounded = Math.hypot(distanceX, distanceY) <= radius
      const offset = row + 1 + x * 4
      const isLightning = insideLightning(x / scale, y / scale)
      pixels[offset] = isLightning ? 167 : 15
      pixels[offset + 1] = isLightning ? 139 : 23
      pixels[offset + 2] = isLightning ? 250 : 42
      pixels[offset + 3] = rounded ? 255 : 0
    }
  }

  const header = Buffer.alloc(13)
  header.writeUInt32BE(size, 0)
  header.writeUInt32BE(size, 4)
  header[8] = 8
  header[9] = 6
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(pixels)),
    chunk('IEND', Buffer.alloc(0))
  ])
}

await mkdir(outputDir, { recursive: true })
for (const size of [180, 192, 512]) {
  await writeFile(path.join(outputDir, `icon-${size}.png`), createPng(size))
}