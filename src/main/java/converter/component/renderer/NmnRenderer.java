package converter.component.renderer;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import converter.component.NmnConverter;
import converter.entity.GraphicElement;
import converter.entity.MuiNote;
import converter.entity.NmnNote;
import glm.vec._2.Vec2;
import glm.vec._3.Vec3;
import uno.glsl.Program;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;
import static uno.buffer.UtilKt.destroyBuffers;

import java.io.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class NmnRenderer {
    IntBuffer VBO;

    private NmnConverter nmnConverter;
    File midiFile;
    List<NmnNote> nmnNoteList;
    List<GraphicElement> elements;
    int section;//每小节容量

    public NmnRenderer(){
        elements = new ArrayList<>();
        midiFile = new File("src/main/resources/symbols/AWM.mid");

        //默认，3/4拍，每小节3*8=24个32分音符
        section = 24;
    }
    public NmnRenderer(File midiFile) {
        elements = new ArrayList<>();
        this.midiFile = midiFile;

        //默认，4/4拍，每小节4*8=32个32分音符
        section = 32;
    }
    public NmnRenderer(File midiFile, String beat) {
        elements = new ArrayList<>();
        this.midiFile = midiFile;

        try {
            int num = Integer.parseInt(beat.substring(0, beat.indexOf("/")));
            int noteType = Integer.parseInt(beat.substring(beat.indexOf("/")));

            int timePerNote = 8;
            switch (noteType) {
                case 1:
                    timePerNote = 32;
                    break;
                case 2:
                    timePerNote = 16;
                    break;
                case 4:
                    timePerNote = 8;
                    break;
                case 8:
                    timePerNote = 4;
                    break;
                case 16:
                    timePerNote = 2;
                    break;
                case 32:
                    timePerNote = 1;
                    break;
            }

            section = num * timePerNote;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            section = 24;
        }
    }

    public void init(GL3 gl) {

        //生成VBO
        VBO = GLBuffers.newDirectIntBuffer(3);
        gl.glGenBuffers(3, VBO);

        //TODO
        initTonality(gl);
        initNotes(gl);


        //gl.glDeleteBuffers(1, VBO);
        //destroyBuffers(VBO);
    }

    public void drawNmn(GL3 gl, Program program) {
        gl.glUseProgram(program.name);

        for(GraphicElement element: elements){
            gl.glBindTexture(GL_TEXTURE_2D, element.getTexture().get(0));
            gl.glBindVertexArray(element.getVao().get(0));

            gl.glUniform1f(program.get("offsetX"), element.getOffsetX());
            gl.glUniform1f(program.get("offsetY"), element.getOffsetY());

            gl.glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        }


    }

    private void initTonality(GL3 gl){
        final float[] triVertices = {
                //   ---- 位置 ----      - 纹理坐标 -
                -0.8f,  0.9f,             1.0f, 1.0f,   // 右上
                -0.8f, 0.75f,             1.0f, 0.0f,   // 右下
                -0.98f, 0.75f,             0.0f, 0.0f,   // 左下
                -0.98f,  0.9f,             0.0f, 1.0f    // 左上
        };
        FloatBuffer vertexBufferTriangle = GLBuffers.newDirectFloatBuffer(triVertices);

        GraphicElement element = new GraphicElement();
        //生成VAO
        gl.glGenVertexArrays(1, element.getVao());

        //绑定VAO
        gl.glBindVertexArray(element.getVao().get(0));
        //绑定VBO
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBO.get(0));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBufferTriangle.capacity() * Float.BYTES, vertexBufferTriangle, GL_STATIC_DRAW);

        //纹理
        //生成纹理
        gl.glGenTextures(1, element.getTexture());

        //绑定纹理
        gl.glBindTexture(GL_TEXTURE_2D, element.getTexture().get(0));
        // 为当前绑定的纹理对象设置环绕、过滤方式
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        //加载图片
        try {
            TextureData textureData = TextureIO.newTextureData(GLProfile.getDefault(), new File("src/main/resources/symbols/tonality.jpg"), false, "JPG");
            if (textureData != null) {
                System.out.println(textureData.getHeight());
                gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, textureData.getWidth(), textureData.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, textureData.getBuffer());
                gl.glGenerateMipmap(GL_TEXTURE_2D);
            } else {
                System.out.println("failed to load picture");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        //-----链接顶点属性-----
        //告诉OpenGL该如何解析顶点数据（应用到逐个顶点属性上）
        //0-1是坐标
        gl.glVertexAttribPointer(0, Vec2.length, GL_FLOAT, false, Vec2.SIZE + Vec2.SIZE, 0);
        gl.glEnableVertexAttribArray(0);

        //2-3是纹理
        gl.glVertexAttribPointer(1, Vec2.length, GL_FLOAT, false, Vec2.SIZE + Vec2.SIZE, Vec2.SIZE);
        gl.glEnableVertexAttribArray(1);

        elements.add(element);
    }

    private void initNotes(GL3 gl){
        nmnConverter = NmnConverter.GetInstance();
        nmnNoteList = nmnConverter.getNmnNoteList(midiFile);

        float[] Vertices = {
                //   ---- 位置 ----      - 纹理坐标 -
                -0.93f,  0.7f,             1.0f, 1.0f,   // 右上
                -0.93f, 0.55f,             1.0f, 0.0f,   // 右下
                -0.98f, 0.55f,             0.0f, 0.0f,   // 左下
                -0.98f,  0.7f,             0.0f, 1.0f    // 左上
        };
        FloatBuffer vertexBufferTriangle = GLBuffers.newDirectFloatBuffer(Vertices);

        float[] underline = {
                //   ---- 位置 ----      - 纹理坐标 -
                -0.93f,  0.7f,             1.0f, 1.0f,   // 右上
                -0.93f, 0.65f,             1.0f, 0.0f,   // 右下
                -0.98f, 0.65f,             0.0f, 0.0f,   // 左下
                -0.98f,  0.7f,             0.0f, 1.0f    // 左上
        };
        FloatBuffer vertexBufferUnderline = GLBuffers.newDirectFloatBuffer(underline);

        float offsetX = 0;
        float offsetY = 0;

        int currentLine = 0;
        int currentSection = 0;
        int sectionContent = 0;
        for(NmnNote nmnNote: nmnNoteList){
            GraphicElement element = new GraphicElement();
            offsetX +=0.05;
            //换行操作
            if(currentSection == 4){
                currentLine++;
                currentSection = 0;
                offsetX = 0.05f;
                offsetY -= 0.23f;
            }
            element.setOffsetX(offsetX);
            element.setOffsetY(offsetY);

            addElement(gl, element, Integer.toString(nmnNote.getPitch()), vertexBufferTriangle, 1);

            //如果是4分音符，在后面加个空格；如果是2分音符或全音符，加横杠
            int time = nmnNote.getTime();
            if(time == 4 ){
                String picName = " ";
                GraphicElement tempElement = new GraphicElement();

                //如果是带附点的
                if(nmnNote.getDotNum()>0){
                    picName = "dot" + nmnNote.getDotNum();
                    offsetX +=0.05;
                    tempElement.setOffsetX(offsetX);
                    tempElement.setOffsetY(offsetY);

                    addElement(gl, tempElement, picName, vertexBufferTriangle, 1);
                }

                picName = "blank";

                offsetX +=0.05;
                tempElement.setOffsetX(offsetX);
                tempElement.setOffsetY(offsetY);

                addElement(gl, tempElement, picName, vertexBufferTriangle, 1);
            }

            if(time == 2){

                GraphicElement tempElement = new GraphicElement();
                offsetX +=0.075;
                tempElement.setOffsetX(offsetX);
                tempElement.setOffsetY(offsetY);

                addElement(gl, tempElement, "rung", vertexBufferTriangle, 1);


                String picName = " ";
                //如果是带一个附点的
                if(nmnNote.getDotNum()==1){
                    picName = "rung";
                }else {
                    picName = "blank";
                }
                //TODO：处理带两个及以上附点的情况
                //TODO：处理跨小节情况

                tempElement = new GraphicElement();
                offsetX +=0.075;
                tempElement.setOffsetX(offsetX);
                tempElement.setOffsetY(offsetY);

                addElement(gl, tempElement, picName, vertexBufferTriangle, 1);

            }

            if(time == 1) {
                for(int i = 0;i<3;i++){
                    //TODO:处理全音符跨小节问题

                    GraphicElement tempElement = new GraphicElement();
                    offsetX += 0.075;
                    tempElement.setOffsetX(offsetX);
                    tempElement.setOffsetY(offsetY);
                    offsetX += 0.025;

                    addElement(gl, tempElement, "rung", vertexBufferTriangle, 1);
                }
            }

            if(time > 4){

                GraphicElement tempElement = new GraphicElement();
                //offsetX不变
                float tempOffsetY = offsetY - 0.15f;
                tempElement.setOffsetX(offsetX);
                tempElement.setOffsetY(tempOffsetY);

                addElement(gl, tempElement, "underline"+time, vertexBufferUnderline, 2);

            }


            //计算当前小节容量，是否开始新的小节
            int tempContent = parseContent(time);

            int tempDotNum = nmnNote.getDotNum();
            while(tempDotNum>0){
                sectionContent += tempContent;
                tempContent = tempContent/2;
                tempDotNum--;
            }
            sectionContent += tempContent;

            //3/4拍的情况下
            if(sectionContent>=section){
                currentSection++;
                //画小节线
                GraphicElement vBar = new GraphicElement();
                offsetX +=0.05;
                vBar.setOffsetX(offsetX);
                vBar.setOffsetY(offsetY);
                addElement(gl,vBar,"vBar",vertexBufferTriangle,1);
                //小节内容清零
                sectionContent = 0;
            }



        }



    }

    private void addElement(GL3 gl, GraphicElement element, String picName, FloatBuffer vertexBufferTriangle, int VBO_Num){
        //生成VAO
        gl.glGenVertexArrays(1, element.getVao());

        //绑定VAO
        gl.glBindVertexArray(element.getVao().get(0));
        //绑定VBO
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBO.get(VBO_Num));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBufferTriangle.capacity() * Float.BYTES, vertexBufferTriangle, GL_STATIC_DRAW);

        //纹理
        //生成纹理
        gl.glGenTextures(1, element.getTexture());

        //绑定纹理
        gl.glBindTexture(GL_TEXTURE_2D, element.getTexture().get(0));
        // 为当前绑定的纹理对象设置环绕、过滤方式
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        //加载图片
        try {
            TextureData textureData = TextureIO.newTextureData(GLProfile.getDefault(), new File("src/main/resources/symbols/"+picName+".jpg"), false, "JPG");
            if (textureData != null) {
                //System.out.println(textureData.getHeight());
                gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, textureData.getWidth(), textureData.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, textureData.getBuffer());
                gl.glGenerateMipmap(GL_TEXTURE_2D);
            } else {
                System.out.println("failed to load picture");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        //-----链接顶点属性-----
        //告诉OpenGL该如何解析顶点数据（应用到逐个顶点属性上）
        //0-1是坐标
        gl.glVertexAttribPointer(0, Vec2.length, GL_FLOAT, false, Vec2.SIZE + Vec2.SIZE, 0);
        gl.glEnableVertexAttribArray(0);

        //2-3是纹理
        gl.glVertexAttribPointer(1, Vec2.length, GL_FLOAT, false, Vec2.SIZE + Vec2.SIZE, Vec2.SIZE);
        gl.glEnableVertexAttribArray(1);

        elements.add(element);
    }

    private int parseContent(int noteTime){
        int tempContent=0;
        switch (noteTime){
            case 1:
                tempContent = 32;
                break;
            case 2:
                tempContent = 16;
                break;
            case 4:
                tempContent = 8;
                break;
            case 8:
                tempContent = 4;
                break;
            case 16:
                tempContent = 2;
                break;
            case 32:
                tempContent = 1;
                break;
        }
        return tempContent;
    }
}