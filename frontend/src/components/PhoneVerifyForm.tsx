import { useEffect, useState } from 'react';
import type { AxiosError } from 'axios';
import { api } from '../api';

interface Props {
  // 인증에 성공하면 부모가 /api/me를 다시 불러 화면을 갱신한다
  onVerified: () => void;
}

// 서버의 RESEND_COOLDOWN과 같은 값. 여기 카운트다운은 안내일 뿐이고,
// 실제로 막는 건 서버다(시계를 조작하거나 새로고침해도 서버가 429를 준다).
const COOLDOWN_SECONDS = 60;

// 백엔드 ApiExceptionHandler가 { message } 형태로 내려준다.
// 등록되지 않은 예외는 메시지가 없으므로 기본 문구로 떨어진다.
function messageOf(error: unknown): string {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? '잠시 후 다시 시도해주세요';
}

export default function PhoneVerifyForm({ onVerified }: Props) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown(cooldown - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const sendCode = async () => {
    setError(null);
    setBusy(true);
    try {
      await api.post('/api/phone/request', { phone });
      setSent(true);
      setCooldown(COOLDOWN_SECONDS);
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  };

  const verify = async () => {
    setError(null);
    setBusy(true);
    try {
      await api.post('/api/phone/verify', { phone, code });
      onVerified();
    } catch (e) {
      setError(messageOf(e));
      setCode(''); // 틀렸으면 비워준다. 남은 시도가 5회뿐이라 오타를 반복하기 쉽다
    } finally {
      setBusy(false);
    }
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (sent) verify();
        else sendCode();
      }}
      style={{ display: 'flex', flexDirection: 'column', gap: '.75rem', maxWidth: '20rem' }}
    >
      <div style={{ display: 'flex', gap: '.5rem' }}>
        <input
          type="tel"
          inputMode="numeric"
          maxLength={11}
          placeholder="01012345678"
          value={phone}
          // 서버는 숫자만 받는다. 하이픈을 넣어도 되게 여기서 걷어낸다.
          onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))}
          disabled={busy}
        />
        <button type="button" onClick={sendCode} disabled={busy || cooldown > 0 || phone.length < 10}>
          {cooldown > 0 ? `재발송 ${cooldown}초` : sent ? '재발송' : '인증번호 받기'}
        </button>
      </div>

      {sent && (
        <div style={{ display: 'flex', gap: '.5rem' }}>
          <input
            inputMode="numeric"
            maxLength={6}
            placeholder="6자리"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
            disabled={busy}
          />
          <button type="submit" disabled={busy || code.length !== 6}>
            확인
          </button>
        </div>
      )}

      {error && (
        <p role="alert" style={{ color: 'crimson', margin: 0 }}>
          {error}
        </p>
      )}
    </form>
  );
}
