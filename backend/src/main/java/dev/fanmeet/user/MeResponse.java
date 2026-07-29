package dev.fanmeet.user;

public record MeResponse(Long id, String nickname, Role role, boolean phoneVerified) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getNickname(), user.getRole(), user.isPhoneVerified());
    }
}
