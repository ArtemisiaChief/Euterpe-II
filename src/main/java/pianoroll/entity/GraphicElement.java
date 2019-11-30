package pianoroll.entity;

import com.jogamp.opengl.util.GLBuffers;

import java.nio.IntBuffer;

public abstract class GraphicElement {

    private IntBuffer vao;

    private int vbo;

    private int trackID;

    private int colorID;

    public GraphicElement(int trackID, int colorID) {
        vao = GLBuffers.newDirectIntBuffer(1);
        this.trackID = trackID;
        this.colorID = colorID;
    }

    public IntBuffer getVao() {
        return vao;
    }

    public void setVbo(int vbo) {
        this.vbo = vbo;
    }

    public int getVbo() {
        return vbo;
    }

    public int getTrackID() {
        return trackID;
    }

    public int getColorID() {
        return colorID;
    }

    protected void setColorID(int colorID) {
        this.colorID = colorID;
    }

    public static boolean IsWhite(int trackID) {
        switch (trackID % 12) {
            case 1:
            case 4:
            case 6:
            case 9:
            case 11:
                return false;
            default:
                return true;
        }
    }

}