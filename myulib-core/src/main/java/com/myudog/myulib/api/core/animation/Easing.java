package com.myudog.myulib.api.core.animation;

public enum Easing {
    LINEAR {
        @Override
        public double apply(double progress) { return clamp(progress); }
    },
    IN_BACK {
        @Override
        public double apply(double t) {
            double s = 1.70158;
            t = clamp(t);
            return t * t * ((s + 1) * t - s);
        }
    },
    OUT_BACK {
        @Override
        public double apply(double t) {
            double s = 1.70158;
            t = clamp(t) - 1;
            return t * t * ((s + 1) * t + s) + 1;
        }
    },
    IN_OUT_BACK {
        @Override
        public double apply(double t) {
            double s = 1.70158 * 1.525;
            t = clamp(t) * 2;
            if (t < 1) return 0.5 * (t * t * ((s + 1) * t - s));
            t -= 2;
            return 0.5 * (t * t * ((s + 1) * t + s) + 2);
        }
    },
    IN_BOUNCE {
        @Override
        public double apply(double t) { return 1 - OUT_BOUNCE.apply(1 - clamp(t)); }
    },
    OUT_BOUNCE {
        @Override
        public double apply(double t) {
            t = clamp(t);
            if (t < (1 / 2.75)) return 7.5625 * t * t;
            else if (t < (2 / 2.75)) return 7.5625 * (t -= (1.5 / 2.75)) * t + 0.75;
            else if (t < (2.5 / 2.75)) return 7.5625 * (t -= (2.25 / 2.75)) * t + 0.9375;
            else return 7.5625 * (t -= (2.625 / 2.75)) * t + 0.984375;
        }
    },
    IN_OUT_BOUNCE {
        @Override
        public double apply(double t) {
            t = clamp(t);
            if (t < 0.5) return IN_BOUNCE.apply(t * 2) * 0.5;
            return OUT_BOUNCE.apply(t * 2 - 1) * 0.5 + 0.5;
        }
    },
    IN_CIRC {
        @Override
        public double apply(double t) { t = clamp(t); return 1 - Math.sqrt(1 - t * t); }
    },
    OUT_CIRC {
        @Override
        public double apply(double t) { t = clamp(t) - 1; return Math.sqrt(1 - t * t); }
    },
    IN_OUT_CIRC {
        @Override
        public double apply(double t) {
            t = clamp(t) * 2;
            if (t < 1) return -0.5 * (Math.sqrt(1 - t * t) - 1);
            t -= 2;
            return 0.5 * (Math.sqrt(1 - t * t) + 1);
        }
    },
    IN_CUBIC {
        @Override
        public double apply(double t) { t = clamp(t); return t * t * t; }
    },
    OUT_CUBIC {
        @Override
        public double apply(double t) { t = clamp(t) - 1; return t * t * t + 1; }
    },
    IN_OUT_CUBIC {
        @Override
        public double apply(double t) {
            t = clamp(t) * 2;
            if (t < 1) return 0.5 * t * t * t;
            t -= 2;
            return 0.5 * (t * t * t + 2);
        }
    },
    IN_ELASTIC {
        @Override
        public double apply(double t) {
            t = clamp(t);
            if (t == 0) return 0; if (t == 1) return 1;
            return -Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.1) * 5 * Math.PI);
        }
    },
    OUT_ELASTIC {
        @Override
        public double apply(double t) {
            t = clamp(t);
            if (t == 0) return 0; if (t == 1) return 1;
            return Math.pow(2, -10 * t) * Math.sin((t - 0.1) * 5 * Math.PI) + 1;
        }
    },
    IN_OUT_ELASTIC {
        @Override
        public double apply(double t) {
            t = clamp(t);
            if (t == 0) return 0; if (t == 1) return 1;
            t *= 2;
            if (t < 1) return -0.5 * Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.1) * 5 * Math.PI);
            return 0.5 * Math.pow(2, -10 * (t - 1)) * Math.sin((t - 1.1) * 5 * Math.PI) + 1;
        }
    },
    IN_EXPO {
        @Override
        public double apply(double t) { t = clamp(t); return t == 0 ? 0 : Math.pow(2, 10 * (t - 1)); }
    },
    OUT_EXPO {
        @Override
        public double apply(double t) { t = clamp(t); return t == 1 ? 1 : 1 - Math.pow(2, -10 * t); }
    },
    IN_OUT_EXPO {
        @Override
        public double apply(double t) {
            t = clamp(t);
            if (t == 0) return 0; if (t == 1) return 1;
            t *= 2;
            if (t < 1) return 0.5 * Math.pow(2, 10 * (t - 1));
            return 0.5 * (2 - Math.pow(2, -10 * (t - 1)));
        }
    },
    IN_QUAD {
        @Override
        public double apply(double t) { t = clamp(t); return t * t; }
    },
    OUT_QUAD {
        @Override
        public double apply(double t) { t = clamp(t); return t * (2 - t); }
    },
    IN_OUT_QUAD {
        @Override
        public double apply(double t) {
            t = clamp(t) * 2;
            if (t < 1) return 0.5 * t * t;
            return -0.5 * ((--t) * (t - 2) - 1);
        }
    },
    IN_QUART {
        @Override
        public double apply(double t) { t = clamp(t); return t * t * t * t; }
    },
    OUT_QUART {
        @Override
        public double apply(double t) { t = clamp(t) - 1; return 1 - t * t * t * t; }
    },
    IN_OUT_QUART {
        @Override
        public double apply(double t) {
            t = clamp(t) * 2;
            if (t < 1) return 0.5 * t * t * t * t;
            t -= 2;
            return -0.5 * (t * t * t * t - 2);
        }
    },
    IN_QUINT {
        @Override
        public double apply(double t) { t = clamp(t); return t * t * t * t * t; }
    },
    OUT_QUINT {
        @Override
        public double apply(double t) { t = clamp(t) - 1; return t * t * t * t * t + 1; }
    },
    IN_OUT_QUINT {
        @Override
        public double apply(double t) {
            t = clamp(t) * 2;
            if (t < 1) return 0.5 * t * t * t * t * t;
            t -= 2;
            return 0.5 * (t * t * t * t * t + 2);
        }
    },
    IN_SINE {
        @Override
        public double apply(double t) { t = clamp(t); return 1 - Math.cos(t * Math.PI / 2); }
    },
    OUT_SINE {
        @Override
        public double apply(double t) { t = clamp(t); return Math.sin(t * Math.PI / 2); }
    },
    IN_OUT_SINE {
        @Override
        public double apply(double t) { t = clamp(t); return -0.5 * (Math.cos(Math.PI * t) - 1); }
    },
    SPRING {
        @Override
        public double apply(double t) {
            t = clamp(t);
            return (Math.sin(t * Math.PI * (0.2 + 2.5 * t * t * t)) * Math.pow(1 - t, 2.2) + t) * (1 + 1.2 * (1 - t));
        }
    },
    SMOOTH_STEP {
        @Override
        public double apply(double t) { t = clamp(t); return t * t * (3 - 2 * t); }
    };

    public abstract double apply(double progress);

    public float apply(float progress) {
        return (float) apply((double) progress);
    }

    public static double clamp(double progress) {
        return Math.max(0.0, Math.min(1.0, progress));
    }
}

