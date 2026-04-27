import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const frontendRoot = path.resolve(__dirname, '..')
const sourceDir = path.join(frontendRoot, 'travel-map', 'dist')
const targetDir = path.join(frontendRoot, 'dist', 'travel-map')

if (!fs.existsSync(sourceDir)) {
  throw new Error(`travel-map build output not found: ${sourceDir}`)
}

fs.rmSync(targetDir, { recursive: true, force: true })
fs.mkdirSync(path.dirname(targetDir), { recursive: true })
fs.cpSync(sourceDir, targetDir, { recursive: true })

console.log(`Synced travel-map dist to ${targetDir}`)
