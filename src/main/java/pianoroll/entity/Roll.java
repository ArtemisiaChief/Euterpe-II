package pianoroll.entity;

public abstract class Roll extends GraphicElement{

    private float offsetY;

    private float scaleY;

    boolean isUpdatingScaleY;

    public Roll(int trackID) {
        super(trackID, trackID+100);

        offsetY = 0.0f;
        scaleY = 1.0f;
    }

    public void update(float deltaY) {
        offsetY += deltaY;

        if (isUpdatingScaleY)
            scaleY += deltaY;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public boolean isUpdatingScaleY() {
        return isUpdatingScaleY;
    }

    public void setUpdatingScaleY(boolean updatingScaleY) {
        isUpdatingScaleY = updatingScaleY;
    }

}