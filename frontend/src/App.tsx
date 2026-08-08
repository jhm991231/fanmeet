import { useEffect, useState } from 'react';
import { api, API_BASE } from './api';
import { clearAccessToken } from './auth';

const PROVIDERS = [
  { id: 'kakao', label: '카카오로 로그인' },
  { id: 'google', label: '구글로 로그인' },
  { id: 'naver', label: '네이버로 로그인' },
];

// 백엔드 MeResponse와 짝을 이루는 타입. 서버가 계약을 바꾸면 여기도 같이 바뀌어야 한다.
interface Me {
  id: number;
  nickname: string;
  role: string;
  phoneVerified: boolean;
}

export default function App() {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  // 메모리의 access 토큰이 없어도 일단 호출한다. 401이 나면 인터셉터가 refresh 쿠키로
  // 재발급받아 재시도하므로, 새로고침해도 로그인이 유지된다. 쿠키까지 없으면 재발급도
  // 401이라 catch로 떨어지고 로그인 화면을 보여준다.
  useEffect(() => {
    api
      .get<Me>('/api/me')
      .then((res) => setMe(res.data))
      .catch(() => setMe(null))
      .finally(() => setLoading(false));
  }, []);

  const logout = async () => {
    try {
      await api.post('/api/auth/logout');
    } finally {
      // 서버 응답과 무관하게 클라이언트 상태는 비운다. 쿠키는 서버가 만료시킨다.
      clearAccessToken();
      setMe(null);
    }
  };

  return (
    <main style={{ fontFamily: 'sans-serif', padding: '2rem' }}>
      <h1>fanmeet</h1>

      {loading ? (
        <p>불러오는 중...</p>
      ) : me ? (
        <>
          <p>
            {me.nickname}님 ({me.role}) — 휴대폰 인증{' '}
            {me.phoneVerified ? '완료' : '미완료'}
          </p>
          <button onClick={logout}>로그아웃</button>
        </>
      ) : (
        // 로그인 시작은 API 호출이 아니라 "백엔드의 출발 창구로 이동"이다.
        // 제공자가 늘어도 달라지는 건 URL 끝의 registrationId 하나뿐이다.
        <nav style={{ display: 'flex', gap: '1rem' }}>
          {PROVIDERS.map(({ id, label }) => (
            <a key={id} href={`${API_BASE}/oauth2/authorization/${id}`}>
              {label}
            </a>
          ))}
        </nav>
      )}
    </main>
  );
}
