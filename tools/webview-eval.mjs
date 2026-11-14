// Evaluate JavaScript in the WebView exposed on http://127.0.0.1:9222.
// Used by tools/emu.sh eval/zoom. Requires Node 22+ (global fetch/WebSocket).
const expression = process.argv[2]
if (!expression) {
    console.error('usage: node tools/webview-eval.mjs <expression>')
    process.exit(2)
}

const response = await fetch('http://127.0.0.1:9222/json')
const targets = await response.json()
const target = targets.find((t) => t.type === 'page') ?? targets[0]
if (!target?.webSocketDebuggerUrl) {
    console.error('No WebView target on :9222. Is the app debuggable and running?')
    process.exit(1)
}

const socket = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve, reject) => {
    socket.onopen = resolve
    socket.onerror = () => reject(new Error('WebSocket connection failed'))
})

const id = 1
const reply = await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('CDP timeout')), 5000)
    socket.onmessage = (event) => {
        const message = JSON.parse(event.data)
        if (message.id === id) {
            clearTimeout(timer)
            resolve(message)
        }
    }
    socket.send(JSON.stringify({
        id,
        method: 'Runtime.evaluate',
        params: { expression, returnByValue: true, awaitPromise: true },
    }))
})

socket.close()

if (reply.error || reply.result?.exceptionDetails) {
    console.error(JSON.stringify(reply.error ?? reply.result.exceptionDetails, null, 2))
    process.exit(1)
}
const value = reply.result?.result?.value
if (value !== undefined) {
    console.log(typeof value === 'string' ? value : JSON.stringify(value))
}
