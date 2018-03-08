/*
    Piano.java - Piano note learning program

    Copyright (C) 2018, Bruce Campbell - bc9500 at outlook dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/> or run

        java -jar piano.jar -copyright

*/

/*
A Java program to help learn sight reading music. Displays random notes, which
you play on an attached midi keyboard. Shows speed and accuracy. You can also
enter letters a,b,c,d,e,f,g on the computer keyboard, to help with learning 
the names of the notes.
*/

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import java.awt.event.*;
import java.awt.Dimension;
import java.util.Arrays;
import java.awt.Image;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;
import javax.imageio.*;
import javax.sound.midi.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Piano { 

    static int[] scaledvalues = { 4, 2, 1 };
    static volatile boolean mute = false;
    static volatile int metronome = 60;
    static volatile boolean playing = false;
    static int key_n = 0;
    static int instrn = 0;
    static Boolean debug = false;
    static volatile long firstkeyhitat = 0;
    static volatile int chordkeys = 0;
    static volatile int[] gotchord = new int[64];
    static volatile boolean redraw = true;

    static String[] keynames = { "C", "F", "B\u266D", "E\u266D", "A\u266D", "D\u266D", "G\u266D", "C\u266D", "G", "D", "A", "E", "B", "F\u266F", "C\u266F" };
    static int[] keyw = { 0,0,0,0,0,0,0,0,1,1,1,1,1,1,1 };
    static int[] keyn = { 0,1,2,3,4,5,6,7,1,2,3,4,5,6,7 };
    static int[][] chordtypes = { { 0 }, { 0, 1 }, { 0, 2 }, { 0, 3 }, { 0, 4 }, { 0, 5 }, { 0, 6 }, { 0, 7 }, { 0, 2, 4 }, { 0, 2, 5 }, { 0, 3, 5 }, { 0, 2, 4, 6 } };
    static int[][] selectchords = new int[2][100];
    static int[] nchords = { 0, 0 };
    static int[] chordoption = { 0, 0 };
    static volatile int alternatestaff = -1;
    static volatile boolean bothclefs = false;
    static int[] notelocations = new int[1000];
    static int accw = 20;
    static int scaled = 2;
    static int notesperbar = 4;
    static int usenote = 0;
    static int transposed = 0;
    static volatile boolean done = false;
    static BufferedImage[] notebi = { null, null, null, null };
    static BufferedImage bracebi = null;
    static BufferedImage treblebi = null;
    static BufferedImage bassbi = null;
    static BufferedImage sharpbi = null;
    static BufferedImage flatbi = null;
    static BufferedImage naturalbi = null;
    static BufferedImage playbi = null;
    static BufferedImage stopbi = null;
    static BufferedImage mutebi = null;
    static BufferedImage soundbi = null;
//    static BufferedImage scalebi = null;
//    static BufferedImage randombi = null;
    static volatile char gotchar = 0;
    static int linespace = 16;
    static int staffoffset = 160;
    static int[] stafflownote = { 25, 13 };
    static MidiChannel[] mChannels;
    static String wkey;
    static int[] minorscale = { 0,2,3,5,7,8,10 }; 
    static int[] majorscale = { 0,2,4,5,7,9,11 };
    static int lownotenum = 21;
    static int silence_note = -1;
    static int onlystaff = -1;
    static int[] accyoff = { 28, 23 };

    static int[] recentnotes = { -1, -1 };
    static int lastnr = 0;
    static int notenum;

    static int[] staffmode = { 0, 0 };
    static int[] minincr = new int[2];
    static int[] maxincr = new int[2];
    static int[] minrun = new int[2];
    static int[] maxrun = new int[2];
    static int[] currun = new int[2];
    static int[] rundir = new int[2];
    static Boolean[] reverserun = new Boolean[2];
    static int[] lastnote = new int[2];
    static Boolean[] okrepeats = new Boolean[2];

    private static class noteval {
        public int note = -1;
        public int len = -1;
        public int acc = 0;
    }

    private static void ChooseChordOption( int staffn, int x ) {
        if( staffn < 0 )
            chordoption[0] = chordoption[1] = x;
        else
            chordoption[staffn] = x;
        if( x == 0 ) ChooseChords( staffn, 0, 1, 0, 1 );   // Notes
        else if( x == 1 ) ChooseChords( staffn, 0, 4, 2, 2 );   // small 2 note
        else if( x == 2 ) ChooseChords( staffn, 0, 10, 2, 2 );   // all 2 note
        else if( x == 3 ) ChooseChords( staffn, 0, 10, 3, 3 );   // 3 note
        else if( x == 4 ) ChooseChords( staffn, 0, 10, 0, 10 );   // everything
    }

    private static void ChooseChords( int staffn, int minlen, int maxlen, int minnotes, int maxnotes ) {
        if( staffn < 0 )
            nchords[0] = nchords[1] = 0;
        else
            nchords[staffn] = 0;
        for( int i=0; i<chordtypes.length; i++ ) {
            if( chordtypes[i].length < minnotes ) continue;
            if( chordtypes[i].length > maxnotes ) continue;
            if( chordtypes[i][ chordtypes[i].length-1 ] < minlen ) continue;
            if( chordtypes[i][ chordtypes[i].length-1 ] > maxlen ) continue;
            if( staffn < 0 ) {
                selectchords[0][ nchords[0]++ ] = i;
                selectchords[1][ nchords[1]++ ] = i;
            }
            else {
                selectchords[staffn][ nchords[staffn]++ ] = i;
            }
        }
    }

    private static void printmessage( JTextArea textarea, String message) {
        textarea.append(message + "\n");
        textarea.setCaretPosition(textarea.getDocument().getLength());
    }

    private static void PrintFile( String filename ) throws IOException {
        BufferedReader txtReader = new BufferedReader(new InputStreamReader( Piano.class.getResourceAsStream( "/piano/" + filename ) ) );
        String line;
        while((line = txtReader.readLine())!=null)
            System.out.println( line );
    }

    private static void FetchImages() {
        notebi[0] = FetchImage( "note.png" );
        notebi[2] = FetchImage( "quarternote.png" );
        bracebi = FetchImage( "brace.png" );
        treblebi = FetchImage( "trebleclef.png" );
        bassbi = FetchImage( "bassclef.png" );
        sharpbi = FetchImage( "sharp.png" );
        flatbi = FetchImage( "flat.png" );
        naturalbi = FetchImage( "natural.png" );

        int save_scaled = scaled;
        scaled = 1;
        playbi = FetchImage( "play.png" );
        stopbi = FetchImage( "stop.png" );
        mutebi = FetchImage( "mute.png" );
        soundbi = FetchImage( "sound.png" );
//        scalebi = FetchImage( "scale.png" );
//        randombi = FetchImage( "random.png" );
        scaled = save_scaled;
    }

    private static void PlayChord( noteval[][][][] chords, int gstaff, int zzz, int sharpenorflatten[], int transposed, boolean play ) {
        int noteno;

        for( int staffn=0; staffn<chords[gstaff].length; staffn++ ) {    
            for( int m=0; m<chords[gstaff][staffn][zzz].length; m++ ) {
                noteno = chords[gstaff][staffn][zzz][m].note;
                if( noteno == -1 ) continue;
                notenum = GetNoteNumber( noteno ) + sharpenorflatten[ noteno%7 ];
                if( play )
                    mChannels[0].noteOn(notenum+transposed, 100);
                else
                    mChannels[0].noteOff(notenum+transposed, 100);
            }
        }
    }

    private static void DrawChordOnStaff( BufferedImage bi1, Graphics2D g2d, BufferedImage bi2, Boolean darken, Boolean ledgerlines, int staffn, noteval[] chord, int[] sharpenorflatten, int notew, int staffy, int drawcolour ) {
        int noteno;
        int minnoteno = 10000;
        int maxnoteno = -1;
        int tm = 0;
        int[] testchord = new int[64];
        int[] shifth = new int[64];
        int noteshift;
        Boolean shifted = false;
 
        g2d.setPaint( new Color( drawcolour ) );

        for( int m=0; m<chord.length; m++ )
             if( chord[m].note != -1 ) {
                 testchord[tm] = chord[m].note;
                 shifth[tm] = 0;
                 tm++;
             }

        if( tm > 1 ) {
            Arrays.sort( testchord, 0, tm );
            int minnote = testchord[0];
            int even = 0;
            int odd = 0;
            for( int i=0; i<tm; i++ )
                if( ((testchord[i]-minnote) % 2) == 0 ) even++;
            odd = tm-even;
            for( int i=0; i<(tm-1); i++ ) {
                if( testchord[i] == testchord[i+1]-1 ) {
                    if( (((testchord[i]-minnote)%2) == 0) && (even >= odd) ) {
                        shifth[i] = testchord[i+1];
                        i++;
                    }
                    else {
                        shifth[i] = testchord[i];
                    }
                }
            }
        }

        for( int m=0; m<chord.length; m++ ) {
            noteno = chord[m].note;
            if( noteno != -1 ) {
                if( sharpenorflatten[m] != 0 ) DrawOneNoteOnStaff( bi1, sharpbi, true, false, staffn, noteno, notew-20/scaled, staffy+(linespace/2-accyoff[1])/scaled, drawcolour );
                noteshift = 0;
                for( int z=0; z<tm; z++ ) {
                    if( shifth[z] == noteno ) {
                        noteshift = notebi[usenote].getWidth()-1;
                        if( usenote == 0 ) noteshift -= 2;    // whole note
                        shifted = true;
                        break;
                    }
                }
                DrawOneNoteOnStaff( bi1, notebi[usenote], true, true, staffn, noteno, notew+noteshift, staffy, drawcolour );
                if( noteno > maxnoteno ) maxnoteno = noteno;
                if( noteno < minnoteno ) minnoteno = noteno;
            }
        }
        if( usenote != 0 ) {            // not a whole note
            int midnote = stafflownote[staffn] + 4;
            int fnote = minnoteno;
            if( Math.abs( midnote-maxnoteno ) > Math.abs( midnote-minnoteno ) ) fnote = maxnoteno;
            int noteloc = LocationOnStaff( staffn, fnote, staffy, (linespace/2)/scaled );
            int xoff = 0;
            int yoff = (2*notebi[usenote].getWidth()/3);
            int elen = (maxnoteno-minnoteno)*linespace/2;
            if( fnote < midnote )
               yoff = (notebi[usenote].getHeight()/3)-(50+elen)/scaled;
            if( (fnote < midnote) || shifted )
               xoff = notebi[usenote].getWidth() - Math.max(1,2/scaled);
            g2d.fillRect( notew+xoff, noteloc+yoff, Math.max(1,2/scaled), (50+elen)/scaled );
        }  
    }

    private static void DrawNotesOnStaff( BufferedImage bi1, Graphics2D g2d, int notesperstaff, noteval[][][][] chords, int gstaff, int notex, int staffx, int staffy ) {
        int staffn;
        int notew;

        int[] sharpenorflatten = { 0,0,0,0,0,0,0,0 };

        g2d.setPaint( new Color( 0 ) );

        for( int i=0; i<notesperstaff; i++ ) {
            for( staffn=0; staffn<chords[gstaff].length; staffn++ ) {
                notew = staffx + notex/scaled + notelocations[i];
                DrawChordOnStaff( bi1, g2d, notebi[usenote], true, true, staffn, chords[gstaff][staffn][i], sharpenorflatten, notew, staffy, 0 );  
                if( (i>1) && (((i-1)%notesperbar)==0) && (staffn==0) ) {
                    g2d.fillRect( staffx + notex/scaled + (notelocations[i-1]+notelocations[i-2]+notebi[usenote].getWidth())/2, staffy, Math.max(1,2/scaled), (staffoffset+4*linespace)/scaled );
                }  
            }
        }
    }

    private static void DrawOneNoteOnStaff( BufferedImage bi1, BufferedImage bi2, Boolean darken, Boolean ledgerlines, int staffn, int noteno, int notew, int staffy, int drawcolour ) {
        int noteloc = LocationOnStaff( staffn, noteno, staffy, (linespace/2)/scaled );

        DrawImageOnImage( bi2, bi1, notew, noteloc, darken, drawcolour );

        if( ledgerlines ) DrawLedgerLines( bi1, notew, noteloc, notebi[usenote].getWidth(), false, staffn, noteno, drawcolour );
    }

    private static String GetNoteName( int noteno ) {
        String s = (char)( (int)('A') + noteno%7 ) + Integer.toString(1+(noteno-2)/7);

        return( s );
    }

    private static int GetNoteNumber( int noteno ) {
        if( noteno < 0 ) return(-1);

        int n = lownotenum + minorscale[ noteno%7] + 12 * (noteno/7);

        return( n );
    }

    private static int LocationOnStaff( int staffn, int noteno, int staffstart, int delta ) {
        int loc = staffstart + (staffn > 0 ? staffoffset/scaled : 0) + (4*linespace/scaled)+(stafflownote[staffn]-noteno)*(linespace/2)/scaled - delta;

        return( loc );
    }

    private static void ScrollChords( noteval chords[][][][] ) {
        for( int i=0; i<chords.length-1; i++ ) {
            for( int j=0; j<chords[0].length; j++ ) {
                for( int k=0; k<chords[0][0].length; k++ ) {
                    for( int m=0; m<chords[0][0][0].length; m++ )     
                        chords[i][j][k][m] = chords[i+1][j][k][m];
                }
            }
        }
    }

    private static void ChooseMode( int staffn, int mode, int minnoten[], int maxnoten[] ) {
        int a = staffn;
        int b = staffn;

        if( staffn < 0 ) {
            a = 0;
            b = 1;
        }
        for( int i=a; i<=b; i++ ) {
            if( mode == 0 )
                RandomMode( i, minnoten, maxnoten );
            else if( mode == 1 )
                RunMode( i, minnoten, maxnoten );
            else if( mode == 2 )
                ScaleMode( i, minnoten, maxnoten );
            staffmode[i] = mode;
        }
    }

    private static void RandomMode( int staffn, int minnoten[], int maxnoten[] ) {
        minincr[staffn] = 0;
        maxincr[staffn] = maxnoten[staffn] - minnoten[staffn];
        minrun[staffn] = 1;
        maxrun[staffn] = 1;
        currun[staffn] = maxrun[staffn];
        rundir[staffn] = 1;
        reverserun[staffn] = false;
        lastnote[staffn] = minnoten[staffn];
        okrepeats[staffn] = false;
    }

    private static void ScaleMode( int staffn, int minnoten[], int maxnoten[] ) {
        RandomMode( staffn, minnoten, maxnoten );
        minincr[staffn] = 1;
        maxincr[staffn] = 1;
        minrun[staffn] = 1 + maxnoten[staffn] - minnoten[staffn];
        maxrun[staffn] = 1 + maxnoten[staffn] - minnoten[staffn];
        currun[staffn] = maxrun[staffn];
        int offs = keynames[ key_n ].charAt(0) - 'C';
        if( offs < 0 ) offs += 7;
        lastnote[staffn] = minnoten[staffn] - 1 + offs;
        okrepeats[staffn] = true;
    }

    private static void RunMode( int staffn, int minnoten[], int maxnoten[] ) {
        RandomMode( staffn, minnoten, maxnoten );
        minincr[staffn] = 1;
        maxincr[staffn] = 2;
        minrun[staffn] = 1;
        maxrun[staffn] = 10;
        currun[staffn] = maxrun[staffn];
        reverserun[staffn] = true;
        okrepeats[staffn] = true;
    }

    private static void SelectNotes( noteval chords[][][][], int ng, int notesperstaff, int nstaffs, int minnoten[], int maxnoten[], Random random, String donotes ) {
        String notename;
        int staffn;
        int noteno = 0;
        int nr;
        int chordn = 0;
        int maxcnote = 0;

        for( int i=0; i<notesperstaff; i++ ) {
            for( int j=0; j<chords[0][0][0].length; j++ )
                chords[ng][0][i][j].note = chords[ng][1][i][j].note = -1;
        }

        for( int i=0; i<notesperstaff; i++ ) {
            while( true ) {
                if( alternatestaff != -1 )
                    staffn = alternatestaff;
                else if( onlystaff == -1 )
                    staffn = random.nextInt( nstaffs );
                else
                    staffn = onlystaff;

                chordn = selectchords[staffn][ random.nextInt( nchords[staffn] ) ];		// select the chord type
                maxcnote = chordtypes[ chordn ][ chordtypes[ chordn ].length-1 ];     // get highest note in chord

                currun[staffn]--;

                if( currun[staffn] <= 0 ) {
                    if( debug ) System.out.format( "Flipping direction for staff %d, lastnote %d, dir %d\n", staffn, lastnote[staffn], rundir[staffn] );
                    currun[staffn] = minrun[staffn];
                    if( maxrun[staffn] > minrun[staffn] ) currun[staffn] += random.nextInt( 1 + maxrun[staffn] - minrun[staffn] );
                    if( reverserun[staffn] )
                        rundir[staffn] *= -1;
                    else
                        lastnote[staffn] = minnoten[staffn] - minincr[staffn]; 
                }

                noteno = lastnote[staffn] + rundir[staffn] * minincr[staffn]; 
                if( maxincr[staffn] > minincr[staffn] ) noteno += rundir[staffn] * random.nextInt( 1 + maxincr[staffn] - minincr[staffn] );

                if( (noteno < minnoten[staffn]) || (noteno > (maxnoten[staffn] - maxcnote))) {
                    if( debug ) System.out.format( "Out of range for staff %d %d, note %d, %d\n", staffn, i, noteno, currun[staffn] );
//                    currun[staffn] = 1;
                    continue;
                }

                if( !okrepeats[staffn] ) {
                    for( nr=0; nr<recentnotes.length; nr++ )
                        if( recentnotes[nr] == noteno ) break;

                    if( nr<recentnotes.length ) continue;
                }

                lastnote[staffn] = noteno;

                notename = GetNoteName( noteno );

                if( ! donotes.equals("") ) {
                    if( donotes.indexOf( notename ) < 0 ) continue;
                }

                break;
            }

            if( (alternatestaff != -1) ) alternatestaff = (alternatestaff+1)%2;

            recentnotes[ (lastnr++) % recentnotes.length ] = noteno;
            for( int j=0; j<chordtypes[ chordn ].length; j++ ) {
                chords[ng][staffn][i][j].note = noteno + chordtypes[ chordn ][j];
            }            
            if( bothclefs && (alternatestaff == 0) ) i--;
        }
    }

    private static BufferedImage FetchImage( String fn ) {
        ImageIcon tmpicon = new ImageIcon( Piano.class.getResource( "/piano/" + fn ) );
        // ImageIcon tmpicon = new ImageIcon( "piano\\" + fn );
        if( scaled > 1)
            tmpicon = new ImageIcon( tmpicon.getImage().getScaledInstance(tmpicon.getIconWidth()/scaled, tmpicon.getIconHeight()/scaled, Image.SCALE_SMOOTH) );
        BufferedImage bi1 = new BufferedImage( tmpicon.getIconWidth(), tmpicon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        tmpicon.paintIcon(null, bi1.createGraphics(), 0,0);  // paint the Icon to the BufferedImage.
        return( bi1 );
    }

    private static void ScrollUp( BufferedImage bi1, Graphics2D g, int y, JPanel panel ) {
        int height = bi1.getHeight();
        int width = bi1.getWidth();

        int d1 = 4;

        try
        {
            for( int i=0; i<y/d1; i++ ) {
                g.drawImage( bi1.getSubimage(0, d1, width, height-d1), 0, 0, null );

                //  if( (i>0) && ((i%2)==0) )

                panel.repaint();
                Thread.sleep(scaled);
            }
        } catch (InterruptedException e) {} ;
    }

    private static void HighlightNote( BufferedImage bi1, int x, int y, int width, int height, Boolean reverse, int hcolour ) {
        int colour;
        int f = 0xFFFFFF;

        if( reverse ) {
            f = hcolour;
            hcolour = 0xFFFFFF;
        }

        for( int w=0; w<width; w++ ) {
            for( int h=0; h<height; h++ ) {
                colour = bi1.getRGB( w+x, h+y );
                colour &= 0xFFFFFF;
                if( colour != f ) continue;
                bi1.setRGB( w+x, h+y, hcolour );
            }
        }
    }

    private static void DrawImageOnImage( BufferedImage bi1, BufferedImage bi2, int x, int y, Boolean darken, int drawcolour ) {
        int h;
        int w;
        int red, red1;
        int colour;

        int height = bi1.getHeight();
        int width = bi1.getWidth();

        if( drawcolour > 0 ) darken = false;

        for( h=0; h<height; h++ ) {
            for( w=0; w<width; w++ ) {
                colour = bi2.getRGB( w+x, h+y );
                colour &= 0xFFFFFF;
                red = (colour & 0xff0000) >> 16;
            colour = bi1.getRGB( w, h );
            red1 = (colour & 0xff0000) >> 16;
        colour &= 0xFFFFFF;
        if( (drawcolour > 0) && (colour == 0xFFFFFF) ) continue;
        if( darken && (red1 > red) ) continue; 
        bi2.setRGB( w+x, h+y, drawcolour > 0 ? drawcolour : colour );
            }
        }
    }

    private static void ExtractImage( BufferedImage bi1, int height, int width, BufferedImage bi2, int x, int y ) {
        int h;
        int w;
        int colour;

        for( h=0; h<height; h++ ) {
            for( w=0; w<width; w++ ) {
                colour = bi2.getRGB( w+x, h+y ) & 0xFFFFFF;
                bi1.setRGB( w, h, colour );
            }
        }
    }

    private static void DrawStaff( BufferedImage bi1, Graphics2D g2d, int x, int y, int width, int extra, BufferedImage clefbi, int clefx, int clefy, int clef, int key_n ) {
        g2d.setPaint ( new Color ( 0, 0, 0 ) );
        for( int i=0; i<5; i++ ) {
            g2d.fillRect ( x, y+(i*linespace/scaled), width, Math.max(1,2/scaled) );
        }
        g2d.fillRect( x, y, Math.max(1,2/scaled), 4*linespace/scaled+1+extra );
        g2d.fillRect( x+width-linespace/scaled, y, Math.max(1,2/scaled), 4*linespace/scaled+1+extra );
        g2d.fillRect( x+width-6/scaled, y, 6/scaled, 4*linespace/scaled+1+extra );
        DrawImageOnImage( clefbi, bi1, x+clefx, y+clefy, true, 0 );
        DrawKeySignature( bi1, x+2*clefx+40/scaled, y, key_n, clef );
    }

    private static void DrawKeySignature( BufferedImage bi1, int x, int y, int key_n, int clef ) {
        int[][] order = { { 4, 7, 3, 6, 2, 5, 1 }, { 8, 5, 9, 6, 3, 7, 4 } };
        int offset = 0;
        if( clef > 0 ) offset = -2;

        for( int i=0; i<keyn[key_n]; i++ ) {
            DrawImageOnImage( (keyw[key_n]==0) ? flatbi : sharpbi, bi1, x+i*accw/scaled, (y+4*linespace/scaled-(order[keyw[key_n]][i]+offset)*(linespace/2)/scaled)-accyoff[keyw[key_n]]/scaled, true, 0 );
        }
    }

    private static void DrawGrandStaff( BufferedImage bi1, Graphics2D g2d, int x, int y, int width, int offset, int key_n ) {
        DrawStaff( bi1, g2d, x, y, width, offset, treblebi, 12/scaled, -24/scaled, 0, key_n );
        DrawStaff( bi1, g2d, x, y+offset, width, 0, bassbi, 12/scaled, 0, 1, key_n );
        DrawImageOnImage( bracebi, bi1, x-26/scaled, y, true, 0 );
    }

    private static void Blacken( BufferedImage bi1, Boolean update, String name ) throws IOException {
        int colour, red, green, blue;
        Color col;
        int k, m;

        for( k=0; k<bi1.getHeight(); k++ ) {
            for( m=0; m<bi1.getWidth(); m++ ) {
                colour = bi1.getRGB( m, k );
                blue = colour & 0xff;
                green = (colour & 0xff00) >> 8;
            red = (colour & 0xff0000) >> 16;
        col = new Color(0, 0, 0);
        if( ((red != 0) || (green != 0) || (blue != 0)) && (red < 16) ) {
            //       red = green = blue = 0;
            //       bi1.setRGB( m, k, col.getRGB() );
        }
        col = new Color(255, 0, 0);
        if( (!((red==0) && (green==0) && (blue==0))) &&
                !((red==255) && (green==255) && (blue==255)) ) {
            //        bi1.setRGB( m, k, col.getRGB() );
        }
            }
        }

        if( update ) {
            File outputfile = new File( "piano\\" + name );
            ImageIO.write(bi1, "png", outputfile);
        }
    }

    private static void DrawLedgerLines( BufferedImage bi1, int x, int y, int width, Boolean erase, int staffn, int noteno, int drawcolour ) {
        int ledgerdir=0, ledgern=0, ledgeroff=0, ledgerstart=0;
        int d;

        if( noteno <= (stafflownote[staffn]-2) ) {
            ledgerdir = 1;
            d = stafflownote[staffn]-noteno;
        }
        else if( noteno >= (stafflownote[staffn]+10) ) {
            d = noteno-(stafflownote[staffn]+8);
        }
        else {
            return;
        }

        ledgeroff = d%2;
        ledgern = d/2;

        Color col;
        if( erase )
            col = new Color(255, 255, 255);
        else
            col = new Color(0, 0, 0);

        ledgerstart = y;

        if( ledgeroff == 0 )
            ledgerstart += (linespace/2)/scaled;
        else if( ledgerdir==0 )
            ledgerstart += linespace/scaled;

        for( int k=0; k<ledgern; k++ ) {
            for( int mm=x-4/scaled; mm<x+width+4/scaled; mm++ ) {
                for( int kk=((scaled<=1) ? 0 : 1); kk<((scaled<=1) ? 3 : 2); kk++ ) {
                    bi1.setRGB( mm, ledgerstart+kk-1, drawcolour > 0 ? drawcolour : col.getRGB() );
                }
            }
            ledgerstart += (ledgerdir==0) ? linespace/scaled : -1*linespace/scaled;
        }
    }

    public static void onExit() {
        done = true;
    }

    public static void main(String args[]) throws IOException, MidiUnavailableException {

        String statusfile = "";
        
        Boolean logit = true;

        int staffx = 40;
        int staffy = 80;
        int staffwidth = 1200;
        int showstats = 0;
        int notespacing;
        int minnotespacing = 48;
        int notexstart = 0;
        int notexend = 16;
        int[] miditonote = new int[127];
        int nstaffs = 2;
        int ngrandstaffs = 0;
        int[] minnoten = { 23, 9 };
        int[] maxnoten = { 37, 23 };
        int [] grandy = { 0,0,0,0 };
        int chordtime = 50;
        int maxnotesperstaff = 10000;
        int[] sharpenorflatten = { 0,0,0,0,0,0,0 };
        int[][] sharpflatorder = { { 1,4,0,3,6,2,5 }, { 5,2,6,3,0,4,1 } };
        int notesperstaff = 0;
        int gstaffoffset = 0;
        int startwait = 0;

//        int chords[][][][] = new int[4][2][200][8];    // grand staffs, staffs, chords per staff, notes per chord
        noteval chords[][][][] = new noteval[4][2][200][8];    // grand staffs, staffs, chords per staff, notes per chord

       for( int i=0; i<chords.length; i++ ) 
            for( int j=0; j<chords[0].length; j++ ) 
                for( int k=0; k<chords[0][0].length; k++ ) 
                    for( int m=0; m<chords[0][0][0].length; m++ )  
                       chords[i][j][k][m] = new noteval(); 

        String donotes = "";

        class CustomKeyListener implements KeyListener{
            public void keyTyped(KeyEvent e) {   
                char ch;

                ch = e.getKeyChar();
                if( (ch >= 'a') && (ch <= 'z') ) ch = (char) (ch & 0x5f);

                if( (ch >= 'A') && (ch <= 'G') ) {
                    int x = minorscale[ ch - 'A' ] + lownotenum - 12;
                    x += 12*((notenum - x)/12);
                    if( Math.abs( notenum-x ) > Math.abs(notenum-(x+12) ) ) x += 12;
                    gotchord[0] = x;
                    chordkeys=1;
                    firstkeyhitat = System.currentTimeMillis();
                    if( ! mute ) mChannels[0].noteOn( silence_note=(x+transposed), 100 );
                }
                else if( ch == 0x1B )
                    done = true;
                wkey = "";
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {            
                if( silence_note != -1 ) {
                    mChannels[0].noteOff( silence_note, 100 );
                    silence_note = -1;
                }
            }    
        } 

        class MidiInputReceiver implements Receiver {
            public String name;
            public MidiInputReceiver(String name) {
                this.name = name;
            }
            public void send(MidiMessage msg, long timeStamp) {
                byte[] data = msg.getMessage();

                if( ((data[0] & 0x90) == ShortMessage.NOTE_ON) && (data[2] > 0) ) {
                    wkey = name;

                    gotchord[chordkeys++] = data[1];
                    firstkeyhitat = System.currentTimeMillis();

                    if( ! mute ) mChannels[0].noteOn( data[1]+transposed, 100 );
                }
                else if((data[0] & 0x80) == ShortMessage.NOTE_OFF) {
                    mChannels[0].noteOff( data[1]+transposed, 100 );
                }
            }
            public void close() {
            }
        }

        ChooseChordOption( -1, 0 );
        ChooseMode( -1, 0, minnoten, maxnoten );

        for( int i=0; i<args.length; i++ ) {
            if( args[i].equals( "-treble") ) {
                logit = false;
                onlystaff = 0;
            }
            else if( args[i].equals( "-bass") ) {
                logit = false;
                onlystaff = 1;
            }
            else if( args[i].equals( "-instr") ) {
                i++;
                instrn = Integer.parseInt( args[i] );
            }
            else if( args[i].equals( "-copyright") ) {
                PrintFile( "copyrightfull.txt" );
                System.exit(0);
            }
            else if( args[i].equals( "-help") ) {
                PrintFile( "help.txt" );
                System.exit(0);
            }
            else if( args[i].equals( "-readme") ) {
                PrintFile( "README.txt" );
                System.exit(0);
            }
            else if( args[i].equals( "-key") )
                key_n = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-scaled") )
                scaled = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-metronome") )
                metronome = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-run") )
                showstats = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-notespacing") )
                minnotespacing = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-whole") )
                usenote = 0;
            else if( args[i].equals( "-quarter") )
                usenote = 2;
            else if( args[i].equals( "-transpose") )
                transposed = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-wait") )
                startwait = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-small2") ) ChooseChordOption( onlystaff, 1 );
            else if( args[i].equals( "-all2") ) ChooseChordOption( onlystaff, 2 );
            else if( args[i].equals( "-3") ) ChooseChordOption( onlystaff, 3 );
            else if( args[i].equals( "-all") ) ChooseChordOption( onlystaff, 4 );
            else if( args[i].equals( "-chord") ) {
                if( onlystaff < 0 )
                    nchords[0] = nchords[1] = 0;
                else
                    nchords[onlystaff] = 0;
                String[] x1 = args[++i].split(" ");
                for( String j1 : x1 ) {
                    if( onlystaff < 0 ) {
                        selectchords[0][ nchords[0]++ ] = Integer.parseInt( j1 );
                        selectchords[1][ nchords[1]++ ] = Integer.parseInt( j1 );
                    }
                    else {
                        selectchords[onlystaff][ nchords[onlystaff]++ ] = Integer.parseInt( j1 );
                    }
                }
            }
            else if( args[i].equals( "-alternate") )
                alternatestaff = 1;
            else if( args[i].equals( "-both") ) {
                alternatestaff = 1;
                onlystaff = -1;
                bothclefs = true;
            }
            else if( args[i].equals( "-debug") )
                debug = true;
            else if( args[i].equals( "-scale") ) ChooseMode( onlystaff, 2, minnoten, maxnoten );
            else if( args[i].equals( "-updown") ) ChooseMode( onlystaff, 1, minnoten, maxnoten );
            else if( args[i].equals( "-only") ) {
                i++;
                logit = false;
                donotes = args[i];
            }
            else if( args[i].equals( "-nolog") )
                logit = false;
            else if( args[i].equals( "-width") )
                staffwidth = Integer.parseInt( args[++i] );
            else if( args[i].equals( "-notes") ) {
                maxnotesperstaff = Integer.parseInt( args[++i] );
            }
        }

        notespacing = minnotespacing;

        PrintFile( "copyright.txt" );

        for( int i=0; i<miditonote.length; i++ ) miditonote[i] = -1;
        for( int i=0; i<=61; i++ ) miditonote[ GetNoteNumber(i) ] = i;

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        Synthesizer midiSynth = MidiSystem.getSynthesizer(); 
        midiSynth.open();

        //get and load default instrument and channel lists
        Instrument[] instr = midiSynth.getDefaultSoundbank().getInstruments();

        mChannels = midiSynth.getChannels();

        MidiDevice device;

        for (int i = 0; i < infos.length; i++) {
            try {
                device = MidiSystem.getMidiDevice(infos[i]);

                if( debug ) System.out.println( "Midi " + infos[i].getName() );

                Transmitter trans = device.getTransmitter();
                trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));

                //open each device
                device.open();
                //if code gets this far without throwing an exception print a success message
                if( debug ) System.out.println(device.getDeviceInfo()+" Was Opened");
            } catch (MidiUnavailableException e) {}
        }


        statusfile = "piano-status.txt";

        FetchImages();

        //    Blacken( bracebi, false, "junkbrace.png" );
        //    Blacken( treblebi, false, "junktreble.png" );
        //    Blacken( bassbi, false, "junkbass.png" );

        File f = new File(statusfile);

        String line;
        int logn = 0;
        String[] log = new String[100000];

        if( f.exists() ) {
            FileReader fr1 = new FileReader(statusfile);
            BufferedReader br1 = new BufferedReader(fr1);
            while((line = br1.readLine())!=null) {
                log[logn++] = line + "";
            }
            br1.close();
        }

        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        JLabel label = new JLabel();
        JButton button = new JButton("");
        JButton mbutton = new JButton("");
//        JButton rbutton = new JButton("");
        JComboBox<String> cbinstr = new JComboBox<>();
        JComboBox<String> cbkey = new JComboBox<>();
        JComboBox<String> cbscaled = new JComboBox<>();
        JComboBox<String> cbtransposed = new JComboBox<>();
        JComboBox<String> cbclef = new JComboBox<>();
        JComboBox<String> cbchord = new JComboBox<>();
        JComboBox<String> cbmode = new JComboBox<>();
        JSlider sdmetronome = new JSlider(JSlider.HORIZONTAL, 30, 150, metronome);

        // JTextArea textarea = new JTextArea(5, 50);
        // textarea.setEditable(false);
        // JScrollPane scrollpane = new JScrollPane(textarea);

        sdmetronome.setMajorTickSpacing(30);
        sdmetronome.setMinorTickSpacing(10);
        sdmetronome.setPaintTicks(true);
        sdmetronome.setPaintLabels(true);

        int z1 = 0;
        for( int j=-12; j<=12; j++ ) {
            String inames[] = { "Unison", "Minor second", "Major second", "Minor third", "Major third", "Perfect fourth", "Augmented fourth", "Perfect fifth", "Minor sixth", "Major sixth", "Minor seventh", "Major seventh", "Octave" };
            cbtransposed.addItem( (j==0) ? inames[0] : ( (j<0) ? "-" : "+") + inames[Math.abs(j)] );
            if( j == transposed ) cbtransposed.setSelectedIndex( z1 );
            z1++;
        }

        for( int j=0; j<instr.length; j++ ) cbinstr.addItem( j + " " + instr[j].getName() );
        for( int j=0; j<keynames.length; j++ ) cbkey.addItem( keynames[j] + ((keyn[j] == 0) ? "" : " - " + (keyn[j] + " " + (keyw[j] == 0 ? "flat" : "sharp" ) + (keyn[j] > 1 ? "s" : "") )) );

        cbmode.addItem( "Random" );
        cbmode.addItem( "Up/Down" );
        cbmode.addItem( "Scales" );
        cbmode.setSelectedIndex( (onlystaff >= 0) ? staffmode[onlystaff] : staffmode[0] );

        cbchord.addItem( "Notes" );
        cbchord.addItem( "Small 2 note chords" );
        cbchord.addItem( "All 2 note chords" );
        cbchord.addItem( "3 note chords" );
        cbchord.addItem( "All supported chords" );
//        cbchord.addItem( "Custom" );
        cbchord.setSelectedIndex( (onlystaff >= 0) ? chordoption[onlystaff] : chordoption[0] );

        cbclef.addItem( "Both clefs" );
        cbclef.addItem( "Treble clef" );
        cbclef.addItem( "Bass clef" );
        cbclef.addItem( "Both clefs - alternate" );
        cbclef.addItem( "Both clefs - together" );
        if( alternatestaff == -1 )
            cbclef.setSelectedIndex( (onlystaff == -1) ? 0 : (onlystaff+1) );
        else
            cbclef.setSelectedIndex( bothclefs ? 4 : 3 );

        String[] scalednames = { "50%", "100%", "200%" };

        for( int j=0; j<scalednames.length; j++ ) {
            cbscaled.addItem( scalednames[j] );
            if( scaledvalues[j] == scaled ) cbscaled.setSelectedIndex( j );
        }

        cbinstr.setSelectedIndex( instrn );
        midiSynth.loadInstrument(instr[instrn]);//load an instrument 
        mChannels[0].programChange(instr[instrn].getPatch().getProgram());

        cbkey.setSelectedIndex( key_n );

        cbinstr.setVisible(true);
        cbkey.setVisible(true);
        cbscaled.setVisible(true);
        cbtransposed.setVisible(true);
        cbclef.setVisible(true);
        cbchord.setVisible(true);
        cbmode.setVisible(true);
        sdmetronome.setVisible(true);
        panel.setPreferredSize(new Dimension(staffwidth+100,600));
        frame.add(panel);

        panel.addKeyListener(new CustomKeyListener());

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                onExit();
            }
            public void windowOpened( WindowEvent e ){
                panel.requestFocus();
            }
        });

        // frame.addComponentListener(new ComponentAdapter( ) {
        //  public void componentResized(ComponentEvent ev) {
        //   System.out.format( "resize\n" );
        //  } });

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        BufferedImage bi;

        bi = new BufferedImage( staffwidth+60, 360, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = bi.createGraphics();

        ImageIcon icon = new ImageIcon(bi);
        label = new JLabel(icon);

        JLabel statslabel = new JLabel( "" );

        statslabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        statslabel.setText( "Piano Note Learner" );

        panel.add(button);
        panel.add(mbutton);
        panel.add(cbmode);
        panel.add(cbclef);
        panel.add(cbchord);
        panel.add(cbinstr);
        panel.add(cbkey);
        panel.add(cbscaled);
        panel.add(sdmetronome);
        panel.add(cbtransposed);

        button.setIcon( new ImageIcon(playbi) );
        mbutton.setIcon( new ImageIcon(soundbi) );
//        rbutton.setIcon( new ImageIcon(randomize ? randombi : scalebi ) );

        panel.add(label);

        //  panel.add(scrollpane, BorderLayout.CENTER);
        //  panel.add(scrollpane);
        panel.add(statslabel);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if( ! playing ) gotchar = 1;
                playing = playing ? false : true;
                button.setIcon( new ImageIcon(playing ? stopbi : playbi ) );
                panel.requestFocus();
            }
        }); 

        mbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mute = mute ? false : true;
                mbutton.setIcon( new ImageIcon(mute ? mutebi : soundbi ) );
                panel.requestFocus();
            }
        }); 

//        rbutton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                randomize = randomize ? false : true;
//                rbutton.setIcon( new ImageIcon(randomize ? randombi : scalebi ) );
//                redraw = true;
//                panel.requestFocus();
//            }
//        }); 

        sdmetronome.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {

                metronome = sdmetronome.getValue();

                panel.requestFocus();
            }
        }); 

        cbinstr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                instrn = cbinstr.getSelectedIndex();
                midiSynth.loadInstrument(instr[instrn]);//load an instrument 
                mChannels[0].programChange(instr[instrn].getPatch().getProgram());
                panel.requestFocus();
            } });

        cbkey.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                key_n = cbkey.getSelectedIndex();
                redraw = true;
                panel.requestFocus();
            } });

        cbclef.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int x = cbclef.getSelectedIndex();
                if( x >= 3 ) {
                    onlystaff = -1;
                    alternatestaff = 1;
                    bothclefs = (x == 3) ? false : true;
                }
                else {
                    bothclefs = false;
                    onlystaff = x - 1;
                    if( onlystaff >= 0 ) cbchord.setSelectedIndex( chordoption[onlystaff] );
                    if( onlystaff >= 0 ) cbmode.setSelectedIndex( staffmode[onlystaff] );
                    alternatestaff = -1;
                }
                redraw = true;
                panel.requestFocus();
            } });

        cbmode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int x = cbmode.getSelectedIndex();
                  ChooseMode( onlystaff, x, minnoten, maxnoten );
                redraw = true;
                panel.requestFocus();
            } });

        cbchord.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int x = cbchord.getSelectedIndex();
                ChooseChordOption( onlystaff, x );
                redraw = true;
                panel.requestFocus();
            } });

        cbtransposed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transposed = cbtransposed.getSelectedIndex()-12;
                panel.requestFocus();
            } });

        cbscaled.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaled = scaledvalues[ cbscaled.getSelectedIndex() ];
                FetchImages();
                redraw = true;
                panel.requestFocus();
            } });

        frame.pack();
        frame.setVisible(true);

        Random random = new Random();

        int staffn;
        int noteno;
        String notename;

        int fails = 0;
        int passes = 0;
        int time1 = 0;

        int zzz=-1;
        int ggg=0;
        Boolean was_error = true;
        int notew = 0;
        int bars;

        while( ! done ) {
           if( startwait > 0 )  try {
                Thread.sleep( startwait*1000 );
                startwait = 0;
            } catch (InterruptedException e) {};
            if( redraw ) {
                staffn = 0;
//                currun[0] = maxrun[0];
//                currun[1] = maxrun[1];
                ChooseMode( 0, staffmode[0], minnoten, maxnoten );
                ChooseMode( 1, staffmode[1], minnoten, maxnoten );
                noteno = minnoten[staffn];

                notexstart = 100 + keyn[key_n]*accw;

                for( int i=0; i<sharpenorflatten.length; i++ ) sharpenorflatten[i] = 0;
                for( int i=0; i<keyn[key_n]; i++ ) sharpenorflatten[ sharpflatorder[keyw[key_n]][i] ] = (keyw[key_n] == 0) ? -1 : 1;

                bars = Math.max( 1, (staffwidth - ((notexstart+notexend)/scaled)) / ((notesperbar*minnotespacing+minnotespacing/2)/scaled) );
                notesperstaff = Math.min( maxnotesperstaff, bars*notesperbar );
                int barwidth = (staffwidth - ((notexstart+notexend)/scaled)) / bars;
                notespacing = 2*scaled*(barwidth / (2*notesperbar+1));

                if( debug ) System.out.println( minnotespacing + " " + notespacing );

                int nl = 0;
                int nw = 0;
                for( int i=0; i<bars; i++ ) {
                     for( int j=0; j<notesperbar; j++ ) {
                         notelocations[nl++] = nw;
                         nw += notespacing/scaled;
                     }
                     nw += (notespacing/2)/scaled;
                }

                ngrandstaffs = 1 * scaled;

                staffy = ((maxnoten[0] - stafflownote[0] - 6)/2) * linespace/scaled;

                gstaffoffset = (2*staffoffset)/scaled;

                for( int ng=0; ng<ngrandstaffs; ng++ ) grandy[ng] = staffy+ng*gstaffoffset;

                if( playing ) logit = false;

                zzz = -1;
                ggg = 0;

                g2d.setPaint ( new Color ( 255, 255, 255 ) );
                g2d.fillRect ( 0, 0, bi.getWidth(), bi.getHeight() );

                for( int ng=0; ng<ngrandstaffs; ng++ )
                    DrawGrandStaff( bi, g2d, staffx, grandy[ng], staffwidth, staffoffset/scaled, key_n );

                for( int ng=0; ng<ngrandstaffs; ng++ )
                    SelectNotes( chords, ng, notesperstaff, nstaffs, minnoten, maxnoten, random, donotes );

                for( int ng=0; ng<ngrandstaffs; ng++ ) {
                    DrawNotesOnStaff( bi, g2d, notesperstaff, chords, ng, notexstart, staffx, grandy[ng] );
                }
                redraw = false;
            }
            zzz++;
            if( zzz >= notesperstaff ) {
                zzz = 0;
                if( (ggg+2) >= ngrandstaffs ) {
                    if( (ngrandstaffs == 1) && (notesperstaff <= 2) ) {
                        g2d.setPaint ( new Color ( 255, 255, 255 ) );
                        g2d.fillRect ( 0, 0, bi.getWidth(), bi.getHeight() );
                    }
                    else
                        ScrollUp( bi, g2d, gstaffoffset, panel );
                    ScrollChords( chords );
                    SelectNotes( chords, ngrandstaffs-1, notesperstaff, nstaffs, minnoten, maxnoten, random, donotes );
                    DrawGrandStaff( bi, g2d, staffx, grandy[ngrandstaffs-1], staffwidth, staffoffset/scaled, key_n );
                    DrawNotesOnStaff( bi, g2d, notesperstaff, chords, ngrandstaffs-1, notexstart, staffx, grandy[ngrandstaffs-1] );
                }
                else {
                    ggg++;
                }
            }


            int nnn = chords[ggg][( chords[ggg][0][zzz][0].note != -1 ) ? 0 : 1][zzz][0].note;     // only needed here for character input mode
            notenum = GetNoteNumber( nnn ) + sharpenorflatten[ nnn%7 ];           // only needed here for character input mode

//            notew = staffx + notexstart/scaled + zzz*notespacing/scaled;
            notew = staffx + notexstart/scaled + notelocations[zzz];

            HighlightNote( bi, notew-(notespacing/(4*scaled)), grandy[ggg]-(3*linespace)/scaled, notespacing/scaled, (10*linespace+staffoffset)/scaled, false, 0xC0C0FF );

            panel.repaint();

            long started_at = System.currentTimeMillis();
            long timex = 0;
            firstkeyhitat = 0;
            chordkeys = 0;

            if( playing ) {
                logit = false;
                if( ! mute ) PlayChord( chords, ggg, zzz, sharpenorflatten, transposed, true );
                try {
                    Thread.sleep( 60000/metronome );
                } catch (InterruptedException e) {};
                PlayChord( chords, ggg, zzz, sharpenorflatten, transposed, false );
                //      gotchord[chordkeys++] = notenum;
                firstkeyhitat = System.currentTimeMillis();
            }
            else
                while( (gotchar == 0) && (chordkeys == 0) && (!done) && (!redraw) ) {
                    try {
                        Thread.sleep(chordtime);
                    } catch (InterruptedException e) {} ;
                    if( chordkeys != 0 ) {
                        timex = System.currentTimeMillis() - firstkeyhitat;
                        if( timex < chordtime ) {
                            try {
                                Thread.sleep(chordtime-timex);
                            } catch (InterruptedException e) {} ;
                        }
                    }
                }

            if( gotchar == 1 ) {
                gotchar = 0;
                zzz--;
                continue;
            }

            if( redraw ) continue;

            if( done ) break;

            was_error = false;

            if( chordkeys > 1 ) Arrays.sort( gotchord, 0, chordkeys );

//            for( int o=0; o<chordkeys; o++ ) System.out.format( "%d ", gotchord[o] );
//            System.out.format( "\n" );

            if( (chordkeys == 4) && (gotchord[0] == 36) && (gotchord[1] == 38) && (gotchord[2] == 83) && (gotchord[3] == 84) ) {
                passes = fails = 0;
                time1 = 0;
                redraw = true;
                statslabel.setText( "" );
                startwait = 1;
                continue;
            }

            if( chordkeys != 0 ) {
                long now = firstkeyhitat;

                int took = (int)(now-started_at);

                int notes_needed = 0;
                int notes_matched = 0;
                notename = "";
                for( int n=0; n<chords[ggg].length; n++ ) {    
                    for( int m=0; m<chords[ggg][n][zzz].length; m++ ) {
                        noteno = chords[ggg][n][zzz][m].note;
                        if( noteno == -1 ) continue;
                        notenum = GetNoteNumber( noteno ) + sharpenorflatten[ noteno%7 ];
                        for( int z2=0; z2<chordkeys; z2++ ) {
                            if( gotchord[z2] == notenum ) {
                                notes_matched++;
                                break;
                            }
                        }
                        if( notename.length() > 0 ) notename += " ";
                        notename += GetNoteName( noteno );
                        if( sharpenorflatten[ noteno%7 ] < 0 ) notename += "-";
                        if( sharpenorflatten[ noteno%7 ] > 0 ) notename += "+";
                        notes_needed++;
                    }
                }
                //      System.out.println( "Needed " + notes_needed + " Got " + chordkeys + " Matched " + notes_matched );

                if( (notes_needed == chordkeys) && (notes_needed == notes_matched) ) {
                    if( logit ) log[logn++] = String.valueOf(now) + ":" + notename + ":" + String.valueOf(took) + ":0:" + wkey + ":";
                    time1 += took;
                    passes++;
                }
                else {
                    fails++;
                    String notehit = "";
                    for( int nk=0; nk<chordkeys; nk++ ) {
                        int wn = miditonote[ gotchord[nk] ];
                        if( wn == -1 ) wn = miditonote[ gotchord[nk]-1 ];
                        if( notehit.length() > 0 ) notehit += " ";
                        notehit += GetNoteName( wn );
                        if( miditonote[ gotchord[nk] ] == -1 ) notehit += "+";
                    }
                    if( logit ) log[logn++] = String.valueOf(now) + ":" + notename + ":" + String.valueOf(now-started_at) + ":-1:" + wkey + ":" + notehit;
                    was_error = true;
                }
                int speedav1 = 0;
                if( passes > 0 ) speedav1 = time1/passes;
                double perc = 100.0 * passes / (passes+fails);
                if( (passes+fails) >= showstats) statslabel.setText( speedav1 + "ms avg, " + passes + "/" + (passes+fails) + " correct " + String.format("%.1f", perc) + "%" );
            }

            HighlightNote( bi, notew-(notespacing/(4*scaled)), grandy[ggg]-(3*linespace)/scaled, notespacing/scaled, (10*linespace+staffoffset)/scaled, true, 0xC0C0FF );

            if( was_error ) {
                noteval[][] echord = new noteval[2][16];
                for( int iii=0; iii<echord.length; iii++ ) 
                    for( int jjj=0; jjj<echord[0].length; jjj++ ) {
                         echord[iii][jjj] = new noteval(); 
//                         echord[iii][jjj].note = -1;
                    }
                int[][] esf = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } };
                for( int nk=0; nk<chordkeys; nk++ ) {
                    int wn = miditonote[ gotchord[nk] ];
                    if( wn == -1 ) wn = miditonote[ gotchord[nk]-1 ];
                    staffn = ((wn <= maxnoten[0]) && (wn >= minnoten[0])) ? 0 : 1; 
                    //        if( wn > maxnoten[staffn] ) wn -= 7;
                    //        if( wn < minnoten[staffn] ) wn += 7;
                    if( wn >= 0 ) {
                        echord[staffn][nk].note = wn;
                        if( miditonote[ gotchord[nk] ] == -1) esf[staffn][nk] = 1;
                    }
                }
                for( int zi=0; zi<2; zi++ )
                    DrawChordOnStaff( bi, g2d, notebi[usenote], true, true, zi, echord[zi], esf[zi], notew, grandy[ggg], 0xFF0000 );
                HighlightNote( bi, notew-(notespacing/(4*scaled)), grandy[ggg]-(3*linespace)/scaled, notespacing/scaled, (10*linespace+staffoffset)/scaled, false, 0xFFC0C0 );
                if( maxnotesperstaff <= 2 ) {
                    panel.repaint();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {} ;
                } 
            }

            //    panel.requestFocus();
        }

        PrintWriter writer = new PrintWriter(statusfile, "UTF-8");
        int k = log.length;
        int i = 0;
        if( logn > k/2 ) {
            i = logn-(k/2);
        }
        for( ; i<logn; i++ )
            writer.println( log[i] );
        writer.close();

        System.exit(0);
    }
}
