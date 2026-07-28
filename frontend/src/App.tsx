import { decodePayload, getAccessToken } from './auth';

export default function App() {
  const token = getAccessToken();
  const payload = token ? decodePayload(token) : null;

  return (
    <main style={{ fontFamily: 'sans-serif', padding: '2rem' }}>
      <h1>fanmeet</h1>
      {payload ? (
        <p>
          로그인됨 — 유저 #{payload.sub} ({payload.role})
        </p>
      ) : (
        // 로그인 시작은 API 호출이 아니라 "백엔드의 출발 창구로 이동"이다
        <a href="http://localhost:8080/oauth2/authorization/kakao">카카오로 로그인</a>
      )}
    </main>
  );
}
