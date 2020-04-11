package converter.entity;

public class MuiNote {
    private int pitch;
    private String timeString;
    private int noteNumbers;
    private double durationTicks;

    public MuiNote(int pitch, String timeString, int noteNumbers, double durationTicks) {
        this.pitch = pitch;
        this.timeString = timeString;
        this.noteNumbers = noteNumbers;
        this.durationTicks = durationTicks;
    }

    public String getPitchString() {
        StringBuilder pitchString=new StringBuilder();
        int smallBracket=0;
        int middleBracket=0;
        String note=null;
        if(pitch==-1){
            note="0";
            for(int i=0;i<noteNumbers;i++)
                pitchString.append(note);
            return pitchString.toString();
        }
        while(pitch>71){
            pitch-=12;
            ++middleBracket;
        }
        while(pitch<60){
            pitch+=12;
            ++smallBracket;
        }
        switch (pitch){
            case 60:
                note="1";
                break;
            case 61:
                note="#1";
                break;
            case 62:
                note="2";
                break;
            case 63:
                note="#2";
                break;
            case 64:
                note="3";
                break;
            case 65:
                note="4";
                break;
            case 66:
                note="#4";
                break;
            case 67:
                note="5";
                break;
            case 68:
                note="#5";
                break;
            case 69:
                note="6";
                break;
            case 70:
                note="#6";
                break;
            case 71:
                note="7";
                break;
        }
        for(int i=0;i<smallBracket;i++)
            pitchString.append("(");
        for(int i=0;i<middleBracket;i++)
            pitchString.append("[");
        for(int i=0;i<noteNumbers;i++)
            pitchString.append(note);
        for(int i=0;i<smallBracket;i++)
            pitchString.append(")");
        for(int i=0;i<middleBracket;i++)
            pitchString.append("]");

        return pitchString.toString();
    }

    public String getTimeString() {
        if(noteNumbers>1){
            StringBuilder newTimeString=new StringBuilder("{");
            newTimeString.append(timeString).append("}");
            return newTimeString.toString();
        }
        return timeString;
    }

    public int getNoteNumbers() {
        return noteNumbers;
    }

    public double getDurationTicks() {
        return durationTicks;
    }
}