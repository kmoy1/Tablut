package tablut;

import org.junit.Test;

import static org.junit.Assert.*;

import ucb.junit.textui;

import java.util.ArrayList;
import java.util.Arrays;

import static tablut.Piece.*;
import static tablut.Board.*;
import static tablut.Square.*;
import static tablut.Move.*;


/**
 * The suite of all JUnit tests for the enigma package.
 *
 * @author Kevin Moy
 */
public class UnitTest {
    /**
     * Run the JUnit tests in this package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /**
     * A dummy test as a placeholder for real ones.
     */
    @Test
    public void dummyTest() {
        assertFalse("There are no unit tests!", false);
    }

    /**
     * Print contents of arrayList.
     */
    public void printArrayList(ArrayList arrL) {
        for (int i = 0; i < arrL.size(); i++) {
            System.out.println(arrL.get(i));
        }
    }

    /**
     * Create empty board (2d square array) for testing
     */
    public Square[][] createTestBoard() {
        Square[][] test = new Square[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                test[col][row] = sq(col, row);
            }
        }
        return test;
    }

    /**
     * Create Board SET TO INITIAL POSIITION (2d piece array)
     */
    public Piece[][] createTestPieces() {
        Piece[][] test = new Piece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Square sqr = sq(col, row);
                if (Arrays.asList(INITIAL_ATTACKERS).contains(sqr)) {
                    test[col][row] = BLACK;
                } else if (Arrays.asList(INITIAL_DEFENDERS).contains(sqr)) {
                    test[col][row] = WHITE;
                } else {
                    test[col][row] = EMPTY;
                }
            }
        }
        test[4][4] = KING;
        return test;
    }

    /**
     * Helper function that creates an empty board.
     * We'll setup positions manually.
     **/
    public Piece[][] createEmptyBd() {
        Piece[][] test = new Piece[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                test[i][j] = EMPTY;
            }
        }
        return test;
    }

    /**
     * Test allSquares helper function's workability
     */
    @Test
    public void testAllBetween() {
        Square[][] test = createTestBoard();
        Square src = test[1][0];
        Square dest1 = test[7][0];
        Square dest2 = test[1][6];
        ArrayList<Square> betweenHoriz = src.allBetween(dest1);
        ArrayList<Square> betweenVert = src.allBetween(dest2);
    }

    /**
     * Print out various board states.
     */
    @Test
    public void testStartingBoard() {
        Board game1 = new Board();
        Board game1Copied = new Board(game1, 1);
        game1.makeMove(mv("d1-3"));
        game1.makeMove(mv("d5-7"));
        assertEquals(3, game1.getPositionStack().size());
        assertEquals(1, game1Copied.getPositionStack().size());
        game1.undo();
        assertEquals(game1.getPositionStack().size(), 2);
    }

    /**
     * Test Neighbors() method. Should return an arraylist
     * with a maximum of 4 directly adjacent squares.
     */
    @Test
    public void testNeighbors() {
        Square[][] test = createTestBoard();
        Square src1 = sq("b1");
        Square src2 = sq("d4");
        ArrayList<Square> tst1 = src1.neighbors();
        ArrayList<Square> tst2 = src2.neighbors();
        printArrayList(tst1);
        printArrayList(tst2);
    }

    /**
     * Test the various legalmoves (helper) functions.
     */
    @Test
    public void testLegalMoves() {
        Piece[][] justWhite = createEmptyBd();
        Board game = new Board();
        game.setPosition(justWhite);
        game.put(KING, sq("d5"));
        game.put(WHITE, sq("c5"));
        System.out.println(game);
        printArrayList((ArrayList<Move>) game.legalMoves(WHITE));
    }

    /**
     * Test if Board's PositionSet works.
     * Equality of Board should be position-based ONLY.
     * This is such that repeated positions draw a red flag.
     **/
    @Test
    public void testPositionSet() {
        Piece[][] test1 = createTestPieces();
        Piece[][] test2 = createTestPieces();
        Piece[][] kingless = createTestPieces();
        kingless[4][4] = EMPTY;
        Board game1 = new Board();
        game1.getPositionHistory().add(new Board());
        assertEquals(game1.getPositionHistory().size(), 1);
        game1.makeMove(mv("d1-3"));
        game1.makeMove(mv("d5-7"));
        assertEquals(game1.getPositionHistory().size(), 3);
    }

    /**
     * Test Stack operations for GameState
     **/
    @Test
    public void testPositionStack() {
        Board game = new Board();
        game.makeMove(mv("d1-3"));
        game.makeMove(mv("d5-7"));
        game.undo();
        for (Board b : game.getPositionStack()) {
            System.out.println(b);
        }
        assertEquals(game.getPositionStack().size(), 2);
    }

    @Test
    public void testUNDO() {
        Board game = new Board();
        game.makeMove(mv("d1-3"));
        Board game2 = new Board(game);
        System.out.println(game.getPositionStack().size());
        System.out.println(game2.getPositionStack().size());

    }


    /**
     * Test a few opening moves and the game' features
     * (undo, repeated positions, illegal moves).
     **/
    @Test
    public void testGame() {
        Board game = new Board();
        game.setMoveLimit(100);
        game.makeMove(mv("d1-3"));
        game.makeMove(mv("d5-7"));
        assertEquals(game.turn(), BLACK);
        assertEquals(game.kingPosition(), sq("e5"));
        game.makeMove(mv("d3-1"));
        assertEquals(game.winner(), null);
        game.makeMove(mv("d7-5"));
        assertEquals(game.winner(), BLACK);
    }

    @Test
    /**Test Move limit functionality **/
    public void testMoveLim() {
        Board game = new Board();
        game.setMoveLimit(2);
        game.makeMove(mv("d1-3"));
        game.makeMove(mv("d5-7"));
        game.makeMove(mv("e1-c"));
        game.makeMove(mv("e5-d"));
        assertTrue(game.winner() == null);
        game.makeMove(mv("f1-i"));
        assertTrue(game.winner() == WHITE);
    }
    /**Test game over by capturing king in the throne. **/
    @Test
    public void testGameOver1() {
        Piece[][] pos = createEmptyBd();
        pos[4][4] = KING;
        pos[3][4] = BLACK;
        pos[5][4] = BLACK;
        pos[4][0] = BLACK;
        pos[4][5] = BLACK;
        Board game = new Board();
        game.setPosition(pos);
        System.out.println(game);
        assertEquals(game.winner(), null);
        game.makeMove(mv("e1-4"));
        assertEquals(game.winner(), BLACK);
    }
    /** Test Game Over by capturing the king on the side thrones. */
    @Test
    public void testGameOver2() {
        Piece[][] pos = createEmptyBd();
        Board game = new Board();
        game.setMoveLimit(100);
        game.setPosition(pos);
        game.put(KING, sq("e6"));
        game.put(BLACK, sq("d6"));
        game.put(BLACK, sq("f6"));
        game.put(BLACK, sq("e8"));
        System.out.println(game);
        assertEquals(game.winner(), null);
        game.makeMove(mv("e8-7"));
        assertEquals(game.winner(), BLACK);
    }

    /** Test special capture where the OCCUPIED THRONE
     * is hostile to the white pieces. **/
    @Test
    public void testSpecialCapture1() {
        Piece[][] pos = createEmptyBd();
        Board game = new Board();
        game.setPosition(pos);
        game.put(KING, sq("e5"));
        game.put(BLACK, sq("d5"));
        game.put(BLACK, sq("e6"));
        game.put(BLACK, sq("e4"));
        game.put(BLACK, sq("g6"));
        game.put(WHITE, sq("f5"));
        System.out.println(game);
        game.makeMove(mv("g6-5"));
        System.out.println(game);
    }
    /** Ensure special capture where the OCCUPIED THRONE
     * DOES NOT have 3 black pieces surrounding it is NOT hostile.**/
    @Test
    public void ensureSpecialCapture2() {
        Piece[][] pos = createEmptyBd();
        Board game = new Board();
        game.setPosition(pos);
        game.put(KING, sq("e5"));
        game.put(BLACK, sq("e6"));
        game.put(BLACK, sq("e4"));
        game.put(BLACK, sq("g6"));
        game.put(WHITE, sq("f5"));
        System.out.println(game);
        game.makeMove(mv("g6-5"));
        System.out.println(game);
    }
    /**Test capturing black pieces with King functionality*/
    @Test
    public void testKingCapture() {
        Piece[][] pos = createEmptyBd();
        Board game = new Board();
        game.setPosition(pos);
        game.put(KING, sq("f5"));
        game.put(BLACK, sq("g5"));
        game.put(WHITE, sq("h4"));
        game.put(BLACK, sq("f4"));
        System.out.println(game);
        game.makeMove(mv("f4-3"));
        game.makeMove(mv("h4-5"));
        System.out.println(game);
    }
}


