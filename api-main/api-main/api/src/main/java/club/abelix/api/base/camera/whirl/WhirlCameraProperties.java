package club.abelix.api.base.camera.whirl;

import lombok.Getter;

@Getter
public class WhirlCameraProperties {

    /**
     * Дистанция игрока от точки наблюдения
     */
    float distance;

    /**
     * Изменение градуса в тик
     */
    float speed;

    /**
     * Максимальный отклон по высоте
     */
    float height;

    public static WhirlCameraProperties newProperties() {
        return new WhirlCameraProperties();
    }

    public static WhirlCameraProperties newDefaultProperties() {
        return newProperties()
                .speed(3)
                .height(3)
                .distance(15);
    }

    public WhirlCameraProperties distance(float dist) {
        this.distance = dist;
        return this;
    }

    public WhirlCameraProperties speed(float speed) {
        this.speed = speed;
        return this;
    }


    public WhirlCameraProperties height(float height) {
        this.height = height;
        return this;
    }

}
