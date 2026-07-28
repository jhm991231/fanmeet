import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAccessToken, setAccessToken } from '../auth';

// 백엔드 OAuth2SuccessHandler가 돌려보내는 착지 지점.
// URL 프래그먼트(#accessToken=...)에서 토큰을 꺼내 메모리에 옮기고 홈으로 보낸다.
export default function OAuthCallback() {
  const navigate = useNavigate();
  const [error, setError] = useState(false);

  useEffect(() => {
    const fragment = new URLSearchParams(window.location.hash.substring(1));
    const token = fragment.get('accessToken');

    if (!token) {
      // StrictMode의 이펙트 2회 실행 대비: 이미 저장했다면 에러가 아니다
      if (!getAccessToken()) setError(true);
      return;
    }

    setAccessToken(token);
    // 주소창·방문기록에 토큰 흔적을 남기지 않는다
    window.history.replaceState(null, '', window.location.pathname);
    navigate('/', { replace: true });
  }, [navigate]);

  return <p>{error ? '로그인에 실패했어요. 다시 시도해주세요.' : '로그인 처리 중...'}</p>;
}
