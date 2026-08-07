import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { getAccessToken, setAccessToken } from './auth';

const BASE_URL = 'http://localhost:8080';
const REFRESH_URL = '/api/auth/refresh';

export const api = axios.create({
  baseURL: BASE_URL,
  // refresh 쿠키를 주고받으려면 필요하다. 오리진이 다르면(5173 → 8080)
  // 이 옵션 없이는 브라우저가 쿠키를 싣지 않는다.
  withCredentials: true,
});

// 재시도한 요청인지 표시하기 위한 확장. 같은 요청을 두 번 재시도하지 않는다.
type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

// ── 요청: access 토큰 자동 첨부 ──────────────────────────────────────
api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── 재발급: 동시에 여러 번 부르지 않는다 ─────────────────────────────
// 서버가 rotation 을 쓰기 때문에 첫 요청이 토큰을 폐기한다. 동시에 세 번 부르면
// 뒤의 둘은 이미 없는 토큰으로 조회해 401 을 받고, 멀쩡한 사용자가 로그아웃된다.
// 진행 중인 Promise 를 공유해 HTTP 는 한 번만 나가게 한다.
let refreshing: Promise<string> | null = null;

function refreshOnce(): Promise<string> {
  if (refreshing) {
    return refreshing;
  }

  refreshing = axios
    .post<{ accessToken: string }>(`${BASE_URL}${REFRESH_URL}`, null, {
      withCredentials: true,
    })
    .then((res) => {
      setAccessToken(res.data.accessToken);
      return res.data.accessToken;
    })
    // 성공이든 실패든 비운다. then 에만 두면 한 번 실패한 뒤 이후 모든 재발급이
    // 죽은 Promise 를 기다리게 된다.
    .finally(() => {
      refreshing = null;
    });

  return refreshing;
}

// ── 응답: 401 이면 재발급 후 원래 요청 재시도 ────────────────────────
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;

    if (error.response?.status !== 401 || !original) {
      throw error;
    }
    // 재발급 요청 자체가 401 이면 더 할 수 있는 게 없다. 여기서 또 재발급을
    // 시도하면 무한루프가 된다.
    if (original.url?.includes(REFRESH_URL)) {
      throw error;
    }
    if (original._retry) {
      throw error;
    }
    original._retry = true;

    const token = await refreshOnce();
    original.headers.Authorization = `Bearer ${token}`;
    return api(original);
  },
);
