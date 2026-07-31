import { useEffect, useState } from 'react';
import { getAccessToken } from './auth';

// 백엔드 MeResponse와 짝을 이루는 타입. 서버가 계약을 바꾸면 여기도 같이 바뀌어야 한다.
interface Me {
  id: number;
  nickname: string;
  role: string;
  phoneVerified: boolean;
}

export default function App() {
  const token = getAccessToken();
  const [me, setMe] = useState<Me | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;

    fetch('http://localhost:8080/api/me', {
      // 여기가 핵심 — 토큰을 Authorization 헤더에 실어 보낸다
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        // fetch는 401·500에도 reject하지 않는다. 상태 코드는 직접 확인해야 한다
        if (!res.ok) throw new Error(`서버가 ${res.status}로 응답했어요`);
        return res.json();
      })
      .then(setMe)
      .catch((e: Error) => setError(e.message));
  }, [token]);

  return (
    <main style={{ fontFamily: 'sans-serif', padding: '2rem' }}>
      <h1>fanmeet</h1>
      {!token ? (
        // 로그인 시작은 API 호출이 아니라 "백엔드의 출발 창구로 이동"이다
        <a href="http://localhost:8080/oauth2/authorization/kakao">카카오로 로그인</a>
      ) : error ? (
        <p style={{ color: 'crimson' }}>{error}</p>
      ) : !me ? (
        <p>불러오는 중...</p>
      ) : (
        <p>
          {me.nickname}님 ({me.role}) — 휴대폰 인증 {me.phoneVerified ? '완료' : '미완료'}
        </p>
      )}
    </main>
  );
}
