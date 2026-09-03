package com.cartvia.cartvia_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cartvia")
public class CartviaProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Payment payment = new Payment();
    private final Device device = new Device();
    private final Trolley trolley = new Trolley();
    private final Websocket websocket = new Websocket();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private int accessTokenExpirationMinutes = 60;
        private int refreshTokenExpirationDays = 30;
        private int resetTokenExpirationMinutes = 30;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    @Getter
    @Setter
    public static class Payment {
        private String webhookSecret;
        private boolean mockEnabled = true;
        private String upiPayeeAddress = "cartvia@razorpay";
        private String upiPayeeName = "CartVia";
    }

    @Getter
    @Setter
    public static class Device {
        private String tokenHeader = "X-Device-Token";
    }

    @Getter
    @Setter
    public static class Trolley {
        private int heartbeatTimeoutSeconds = 180;
    }

    @Getter
    @Setter
    public static class Websocket {
        private String endpoint = "/ws";
        private String allowedOrigins = "*";
    }
}
