import http, { extractData } from './index'

/** 发送漂流瓶 */
export function sendBottle(data: { textContent: string; imageUrl?: string; isAnonymous: number }) {
  return http.post('/bottle/send', data).then(extractData)
}

/** 随机拾取漂流瓶 */
export function pickBottle() {
  return http.get('/bottle/pick').then(extractData)
}

/** 点赞漂流瓶 */
export function likeBottle(id: number) {
  return http.post(`/bottle/${id}/like`).then(extractData)
}

/** 举报漂流瓶 */
export function reportBottle(id: number) {
  return http.post(`/bottle/${id}/report`).then(extractData)
}

/** 我的漂流瓶 */
export function getMyBottles() {
  return http.get('/bottle/my').then(extractData)
}

/** 我点赞过的漂流瓶 */
export function getMyLikedBottles() {
  return http.get('/bottle/liked').then(extractData)
}

/** 获取指定漂流瓶详情（用于消息跳转查看） */
export function getBottleDetail(id: number) {
  return http.get(`/bottle/${id}`).then(extractData)
}
