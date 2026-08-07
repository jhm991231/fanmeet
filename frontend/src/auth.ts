// access 토큰은 의도적으로 "모듈 변수(메모리)"에만 보관한다.
// localStorage에 두지 않는 이유: XSS로 통째로 털리는 저장소이기 때문.
// 새로고침하면 사라지지만 refresh 쿠키는 살아 있으므로, api.ts의 인터셉터가
// 첫 401에서 재발급받아 조용히 복구한다.
let accessToken: string | null = null;

export function setAccessToken(token: string) {
  accessToken = token;
}

export function clearAccessToken() {
  accessToken = null;
}

export function getAccessToken() {
  return accessToken;
}

export interface JwtPayload {
  sub: string;
  role: string;
  exp: number;
}

// 페이로드는 암호화가 아니라 base64url 인코딩일 뿐이라 클라이언트에서도 읽을 수 있다.
// (읽을 수 있다 ≠ 위조할 수 있다 — 위조는 서명 검증에서 걸린다)
export function decodePayload(token: string): JwtPayload | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}
