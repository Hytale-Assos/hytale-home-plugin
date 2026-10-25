package assos.hytale.home.home;

public final class Home {

    private String name;
    private String world;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;
    private float roll;
    private long createdAt;

    public Home() {
    }

    public Home(String name, String world, double x, double y, double z, float pitch, float yaw, float roll,
            long createdAt) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
