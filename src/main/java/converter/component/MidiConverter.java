package converter.component;

import converter.entity.MuiNote;
import midipaser.component.MidiParser;
import converter.entity.MidiChannel;
import midipaser.entity.MidiContent;
import midipaser.entity.MidiEvent;
import midipaser.entity.MidiTrack;
import midipaser.entity.events.BpmEvent;
import midipaser.entity.events.InstrumentEvent;
import midipaser.entity.events.NoteEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MidiConverter {

    private static final MidiConverter instance = new MidiConverter();

    public static MidiConverter GetInstance() {
        return instance;
    }

    private MidiConverter() {
    }

    private int resolution;

    private MuiNote muiNote = null;

    private StringBuilder note = null;
    private StringBuilder time = null;
    private StringBuilder front = null;
    private StringBuilder latter = null;
    private StringBuilder mui = null;

    private boolean sameTime = false;

    private int noteCount = 0;

    public String converterToMui(File midiFile) {
        MidiParser parser = MidiParser.GetInstance();
        try {

            MidiContent midiContent = parser.parse(midiFile);
            resolution = midiContent.getResolution();
            List<MidiChannel> midiChannels = sortMidiChannel(midiContent.getMidiTrackList());
            mui = new StringBuilder();

            for (MidiChannel midiChannel : midiChannels) {
                mui.append("paragraph Track" + midiChannel.getTrackNumber()
                        + "Channel" + midiChannel.getChannelNumber() + "\n" + "1=C\n");
                noteCount = 0;
                double currentTick = 0, lastDuration = 0;
                sameTime = false;
                note = new StringBuilder();
                time = new StringBuilder();
                front = new StringBuilder();
                latter = new StringBuilder();
                muiNote = null;
                int end = midiChannel.getMidiEventList().size();
                for (MidiEvent midiEvent : midiChannel.getMidiEventList()) {

                    if (noteCount >= 10 && !sameTime) {
                        mui.append(front).append("  <").append(latter).append(">\n");
                        front.delete(0, front.length());
                        latter.delete(0, latter.length());
                        noteCount = 0;
                    }

                    if (midiEvent instanceof BpmEvent) {
                        BpmEvent bpmEvent = (BpmEvent) midiEvent;
                        changeStatusAddNote();
                        mui.append("speed=" + String.format("%.1f", bpmEvent.getBpm()) + "\n");
                    } else if (midiEvent instanceof InstrumentEvent) {
                        InstrumentEvent instrumentEvent = (InstrumentEvent) midiEvent;
                        changeStatusAddNote();
                        if (midiChannel.getChannelNumber() == 9)
                            mui.append("instrument= -1\n");
                        else
                            mui.append("instrument=" + instrumentEvent.getInstrumentNumber() + "\n");
                    } else {
                        NoteEvent noteEvent = (NoteEvent) midiEvent;
                        if (noteEvent.getTriggerTick() == currentTick && lastDuration != 0) {
                            sameTime = true;
                            MuiNote tempMuiNote = getMuiNote(noteEvent.getPitch(), noteEvent.getDurationTicks());
                            if (tempMuiNote.getDurationTicks() >= muiNote.getDurationTicks()) {
                                note.append(tempMuiNote.getPitchString());
                                time.append(tempMuiNote.getTimeString());
                                noteCount += tempMuiNote.getNoteNumbers();
                            } else {
                                note.append(muiNote.getPitchString());
                                time.append(muiNote.getTimeString());
                                noteCount += muiNote.getNoteNumbers();
                                muiNote = tempMuiNote;
                                lastDuration = muiNote.getDurationTicks();
                            }

                        } else {
                            if (currentTick + lastDuration == noteEvent.getTriggerTick()) {
                                addNote();
                                currentTick = noteEvent.getTriggerTick();
                                lastDuration = noteEvent.getDurationTicks();
                                muiNote = getMuiNote(noteEvent.getPitch(), noteEvent.getDurationTicks());
                            } else if (currentTick + lastDuration < noteEvent.getTriggerTick()) {
                                addNote();
                                MuiNote restNote = getMuiNote(-1, noteEvent.getTriggerTick() - currentTick - lastDuration);
                                front.append(restNote.getPitchString());
                                latter.append(restNote.getTimeString());
                                noteCount += restNote.getNoteNumbers();
                                currentTick = noteEvent.getTriggerTick();
                                lastDuration = noteEvent.getDurationTicks();
                                muiNote = getMuiNote(noteEvent.getPitch(), noteEvent.getDurationTicks());
                            } else {
                                MuiNote restNote = getMuiNote(-1, noteEvent.getTriggerTick() - currentTick);

                                note.insert(0, "|" + restNote.getPitchString() + muiNote.getPitchString()).append("|");
                                time.insert(0, restNote.getTimeString() + muiNote.getTimeString());
                                front.append(note);
                                note.delete(0, note.length());
                                latter.append(time);
                                time.delete(0, time.length());
                                noteCount += muiNote.getNoteNumbers() + restNote.getNoteNumbers();
                                sameTime = false;

                                currentTick = noteEvent.getTriggerTick();
                                lastDuration = noteEvent.getDurationTicks();
                                muiNote = getMuiNote(noteEvent.getPitch(), noteEvent.getDurationTicks());
                            }
                        }
                    }

                    --end;
                    if (end == 0) {
                        if (noteCount >= 10 && !sameTime) {
                            mui.append(front).append("  <").append(latter).append(">\n");
                            front.delete(0, front.length());
                            latter.delete(0, latter.length());
                        }
                        if (sameTime) {
                            note.insert(0, "|" + muiNote.getPitchString()).append("|");
                            time.insert(0, muiNote.getTimeString());
                            front.append(note);
                            latter.append(time);
                            note.delete(0, note.length());
                            time.delete(0, time.length());
                            sameTime = false;
                        } else {
                            front.append(muiNote.getPitchString());
                            latter.append(muiNote.getTimeString());
                        }
                        mui.append(front).append("  <").append(latter).append(">\n");
                        front.delete(0, front.length());
                        latter.delete(0, latter.length());
                    }
                }
                mui.append("end\n\n\n");
            }
            mui.append("\nplay(");
            for (int i = 0; i < midiChannels.size(); ++i) {
                if (i != 0)
                    mui.append("&");
                mui.append("Track").append(midiChannels.get(i).getTrackNumber()).append("Channel").append(midiChannels.get(i).getChannelNumber());
            }
            mui.append(")");


            String result = mui.toString();

            for (int i = 0; i < 5; i++)
                result = result.replaceAll("\\)\\(", "").replaceAll("\\]\\[", "");


            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "转换过程中出现错误，所选取文件为不支持的midi文件";
        }
    }

    private MuiNote getMuiNote(int pitch, double durationTicks) {
        int noteNumbers = 0;
        double remainTick = durationTicks + 1;  //部分midi文件会出现durationTicks少1的情况，这里加上
        double minDurationTicks = 0;
        StringBuilder timeString = new StringBuilder();
        while (remainTick != 0 && remainTick != 1) {
            if (remainTick >= resolution * 6) {
                ++noteNumbers;
                timeString.insert(0, "1*");
                minDurationTicks = resolution * 6;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 4) {
                ++noteNumbers;
                timeString.insert(0, "1");
                minDurationTicks = resolution * 4;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 3) {
                ++noteNumbers;
                timeString.insert(0, "2*");
                minDurationTicks = resolution * 3;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 2) {
                ++noteNumbers;
                timeString.insert(0, "2");
                minDurationTicks = resolution * 2;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 1.5) {
                ++noteNumbers;
                timeString.insert(0, "4*");
                minDurationTicks = resolution * 1.5;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution) {
                ++noteNumbers;
                timeString.insert(0, "4");
                minDurationTicks = resolution;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 0.75) {
                ++noteNumbers;
                timeString.insert(0, "8*");
                minDurationTicks = resolution * 0.75;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 0.5) {
                ++noteNumbers;
                timeString.insert(0, "8");
                minDurationTicks = resolution * 0.5;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 0.375) {
                ++noteNumbers;
                timeString.insert(0, "g*");
                minDurationTicks = resolution * 0.375;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 0.25) {
                ++noteNumbers;
                timeString.insert(0, "g");
                minDurationTicks = resolution * 0.25;
                remainTick -= minDurationTicks;
            } else if (remainTick >= resolution * 0.125) {
                ++noteNumbers;
                timeString.insert(0, "w");
                minDurationTicks = resolution * 0.125;
                remainTick -= minDurationTicks;
            } else {
                //时值少于32分音符的按32分音符处理
                ++noteNumbers;
                timeString.insert(0, "w");
                minDurationTicks = resolution * 0.125;
                remainTick = 0;
            }
        }
        return new MuiNote(pitch, timeString.toString(), noteNumbers, minDurationTicks);
    }

    private List<MidiChannel> sortMidiChannel(List<MidiTrack> midiTracks) {
        List<MidiChannel> midiChannels = new ArrayList<>();
        List<BpmEvent> bpmEvents = new ArrayList<>();
        for (MidiTrack midiTrack : midiTracks) {
            for (MidiEvent midiEvent : midiTrack.getMidiEventList()) {

                if (midiEvent instanceof BpmEvent) {
                    BpmEvent tempBpmEvent = (BpmEvent) midiEvent;
                    boolean repeat = false;
                    for (BpmEvent bpmEvent : bpmEvents) {
                        if (tempBpmEvent.getBpm() == bpmEvent.getBpm())
                            repeat = true;
                    }
                    if (!repeat)
                        bpmEvents.add(tempBpmEvent);
                } else if (midiEvent instanceof InstrumentEvent) {
                    InstrumentEvent tempInstrumentEvent = (InstrumentEvent) midiEvent;
                    MidiChannel tempMidiChannel = null;
                    boolean hasChannel = false;
                    for (MidiChannel midiChannel : midiChannels) {
                        if (midiChannel.getTrackNumber() == midiTrack.getTrackNumber() &&
                                midiChannel.getChannelNumber() == tempInstrumentEvent.getChannel()) {
                            hasChannel = true;
                            tempMidiChannel = midiChannel;
                        }
                    }
                    if (!hasChannel) {
                        tempMidiChannel = new MidiChannel(midiTrack.getTrackNumber(), tempInstrumentEvent.getChannel());
                        midiChannels.add(tempMidiChannel);
                    }
                    tempMidiChannel.getMidiEventList().add(tempInstrumentEvent);
                } else {
                    NoteEvent tempNoteEvent = (NoteEvent) midiEvent;
                    MidiChannel tempMidiChannel = null;
                    boolean hasChannel = false;
                    for (MidiChannel midiChannel : midiChannels) {
                        if (midiChannel.getTrackNumber() == midiTrack.getTrackNumber() &&
                                midiChannel.getChannelNumber() == tempNoteEvent.getChannel()) {
                            hasChannel = true;
                            tempMidiChannel = midiChannel;
                        }
                    }
                    if (!hasChannel) {
                        tempMidiChannel = new MidiChannel(midiTrack.getTrackNumber(), tempNoteEvent.getChannel());
                        midiChannels.add(tempMidiChannel);
                    }
                    tempMidiChannel.getMidiEventList().add(tempNoteEvent);
                }
            }
        }
        for (MidiChannel midiChannel : midiChannels) {
            for (BpmEvent bpmEvent : bpmEvents) {
                for (int i = 0; i < midiChannel.getMidiEventList().size(); ++i) {
                    if (midiChannel.getMidiEventList().get(i).getTriggerTick() >= bpmEvent.getTriggerTick()) {
                        midiChannel.getMidiEventList().add(i, bpmEvent);
                        break;
                    }
                }
            }
        }
        return midiChannels;
    }

    private void changeStatusAddNote() {
        if (muiNote != null) {
            if (sameTime) {
                note.insert(0, "|" + muiNote.getPitchString()).append("|");
                time.insert(0, muiNote.getTimeString());
                front.append(note);
                latter.append(time);
                note.delete(0, note.length());
                time.delete(0, time.length());
                noteCount += muiNote.getNoteNumbers();
                muiNote = null;
                sameTime = false;
            } else {
                front.append(muiNote.getPitchString());
                latter.append(muiNote.getTimeString());
                noteCount += muiNote.getNoteNumbers();
                muiNote = null;
            }
        }
        if (noteCount != 0) {
            mui.append(front).append("  <").append(latter).append(">\n");
            front.delete(0, front.length());
            latter.delete(0, latter.length());
            noteCount = 0;
            mui.append("\n");
        }
    }

    private void addNote() {
        if (sameTime) {
            note.insert(0, "|" + muiNote.getPitchString()).append("|");
            time.insert(0, muiNote.getTimeString());
            front.append(note);
            note.delete(0, note.length());
            latter.append(time);
            time.delete(0, time.length());
            noteCount += muiNote.getNoteNumbers();
            sameTime = false;
        } else if (muiNote != null) {
            front.append(muiNote.getPitchString());
            latter.append(muiNote.getTimeString());
            noteCount += muiNote.getNoteNumbers();
        }
    }

}
