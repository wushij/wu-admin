import {
  connectMessageWebSocket,
  disconnectMessageWebSocket,
  onMessageWebSocket,
  type WsPushMessage,
} from '@/utils/webSocket'

export function useWebSocket() {
  function connect() {
    connectMessageWebSocket()
  }

  function disconnect() {
    disconnectMessageWebSocket()
  }

  function onMessage(handler: (msg: WsPushMessage) => void) {
    return onMessageWebSocket(handler)
  }

  return { connect, disconnect, onMessage }
}
