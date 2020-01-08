package tablut;




import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Stack;
import java.util.Arrays;
import java.util.List;

import static tablut.Piece.*;
import static tablut.Square.*;
import static tablut.Move.mv;


/**
 * The state of a Tablut Game. Because of static nature of stack and set,
 * while creating multiple board instances is allowed, only one such
 * instance may be used as game simulation.
 *
 * @author Kevin Moy
 */
class Board {

    /**
     * The number of squares on a side of the board.
     */
    static final int SIZE = 9;

    /**
     * The throne (or castle) square and its four surrounding squares..
     */
    static final Square THRONE = sq(4, 4),
            NTHRONE = sq(4, 5),
            STHRONE = sq(4, 3),
            WTHRONE = sq(3, 4),
            ETHRONE = sq(5, 4);

    /** Given thrones = d5, e5, f5, e4, e6. **/
    static final Square[] THRONES = {THRONE,
        NTHRONE, STHRONE, WTHRONE, ETHRONE};

    /**
     * Initial positions of attackers. (Black pieces)
     */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /**
     * Initial positions of defenders of the king. (White pieces)
     */
    static final Square[] INITIAL_DEFENDERS = {NTHRONE, ETHRONE,
        STHRONE, WTHRONE, sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)};

    /**
     * Initializes a game board with SIZE squares on a side in the
     * initial position.
     */
    Board() {
        init();
    }

    /**
     * Initializes a copy of MODEL. However, such copies will have
     * empty stacks and sets.
     */
    Board(Board model) {
        copy(model);
    }

    /**
     * Initializes a completely independent, FULL copy of
     * MODEL. Stacks and sets are deep-copied.
     * Use this version for testing AI.
     */
    Board(Board model, int unused) {
        fullCopy(model);
    }

    /**
     * Copies MODEL into me, but treat this more as a "load game snapshot" than
     * a full copy. the position and game circumstance "snapshot" is loaded in.
     * Position stack/set is treated as static and is untouched.
     */
    void copy(Board model) {
        if (model == this) {
            return;
        }
        setFullState(model._turn, model._moveCount,
                model._repeated, model._winner, model._pieceState);
    }

    /** Copies ALL of MODEL into me- stack, set and all.*/
    void fullCopy(Board model) {
        if (model == this) {
            return;
        }
        setFullState(model._turn, model._moveCount,
                model._repeated, model._winner, model._pieceState);
        positionStack.addAll(model.positionStack);
        positionHistory.addAll(model.positionHistory);
    }


    /**
     * Clears the board to the initial position.
     */
    void init() {
        this._turn = BLACK;
        this._moveCount = 0;
        this._repeated = false;
        this._winner = null;
        this._pieceState = new Piece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Square sqr = sq(col, row);
                if (Arrays.asList(INITIAL_ATTACKERS).contains(sqr)) {
                    _pieceState[col][row] = BLACK;
                } else if (Arrays.asList(INITIAL_DEFENDERS).contains(sqr)) {
                    _pieceState[col][row] = WHITE;
                } else {
                    _pieceState[col][row] = EMPTY;
                }
            }
        }
        lim = Integer.MAX_VALUE;
        _pieceState[4][4] = KING;
        positionHistory.clear();
        positionStack.clear();
        positionHistory.add(new Board(this));
        positionStack.push(new Board(this));
    }

    /**
     * Set state of board to exact parameters. Makes a new copy of
     * the pieceState attribute- a new position copy.
     * @param turn turn
     * @param moveCt move count
     * @param repeated whether position repeated
     * @param winner winner
     * @param state current position
     **/
    void setFullState(Piece turn, int moveCt,
                      boolean repeated, Piece winner, Piece[][] state) {
        this._turn = turn;
        this._moveCount = moveCt;
        this._repeated = repeated;
        this._winner = winner;
        this._pieceState = new Piece[SIZE][SIZE];
        for (int i = 0; i < state.length; i++) {
            _pieceState[i] = Arrays.copyOf(state[i], state[i].length);
        }
    }
    /**
     * Set the move limit to LIM.  It is an error if 2*LIM <= moveCount().
     * @param n Max number of moves.
     */
    void setMoveLimit(int n) {
        if (2 * n < moveCount()) {
            throw new IllegalArgumentException();
        }
        lim = n;
    }

    /**
     * Set position of board.
     * @param pos position to set to.
     */
    void setPosition(Piece[][] pos) {
        this._pieceState = pos;
    }

    /**
     * Return a Piece representing whose move it is (WHITE or BLACK).
     */
    Piece turn() {
        return _turn;
    }

    /**
     * Return the winner in the current position, or null if there is no winner
     * yet.
     */
    Piece winner() {
        return _winner;
    }

    /**
     * Returns true iff this is a win due to a repeated position.
     */
    boolean repeatedPosition() {
        return _repeated;
    }

    /**
     * Record current position and set winner() next mover if the current
     * position is a repeat.
     */
    private void checkRepeated() {
        if (positionHistory.contains(this)) {
            this._repeated = true;
            this._winner = _turn;
        }
    }

    /**
     * See if the move limit has been violated after last move.
     */
    private void checkMoves() {
        int movesMade = movesMade(_turn.opponent());
        if (movesMade > lim) {
            this._winner = _turn;
        }
    }

    /**
     * Return the number of moves since the initial position that have not been
     * undone.
     */
    int moveCount() {
        return _moveCount;
    }

    /**
     * Return location of the king.
     */
    Square kingPosition() {
        for (Square s : SQUARE_LIST) {
            if (get(s) == KING) {
                return s;
            }
        }
        return null;
    }

    /**
     * Return the contents the square at S.
     */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /**
     * Return the contents of the square at (COL, ROW), where
     * 0 <= COL, ROW < 9.
     */
    final Piece get(int col, int row) {
        return _pieceState[col][row];
    }

    /**
     * Return the contents of the square at COL ROW.
     */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /**
     * Set square S to P. NOTE: Second half of a move (replace src square
     * with empty first).
     */
    final void put(Piece p, Square s) {
        int col = s.col();
        int row = s.row();
        _pieceState[col][row] = p;
    }

    /**
     * Set square S to P and record for undoing:
     * In my implementation UNDOING info is done in makeMove.
     */
    final void revPut(Piece p, Square s) {
        put(p, s);
    }

    /**
     * Set square COL ROW to P.
     */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /**
     * Return true iff FROM - TO is an unblocked rook move on the current
     * board.  For this to be true, FROM-TO must be a rook move and the
     * squares along it, other than FROM, must be empty.
     */
    boolean isUnblockedMove(Square from, Square to) {
        ArrayList<Square> unblockedSqrs = from.allBetween(to);
        if (unblockedSqrs.size() == 0) {
            return false;
        }
        for (Square s : unblockedSqrs) {
            if (get(s) != EMPTY) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true iff FROM is a valid starting square for a move.
     */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /**
     * Return true iff FROM-TO is a valid move.
     */
    boolean isLegal(Square from, Square to) {
        if (to == THRONE && get(from) != KING) {
            System.out.println(mv(from, to));
            return false;
        }
        boolean legal =  isLegal(from) && isUnblockedMove(from, to);
        if (!legal) {
            System.out.println(mv(from, to));
        }
        return legal;
    }

    /**
     * Return true iff MOVE is a legal move in the current
     * position.
     */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /**
     * Move FROM-TO, assuming this is a legal move.
     */
    void makeMove(Square from, Square to) {
        assert isLegal(from, to);
        put(get(from), to);
        _pieceState[from.col()][from.row()] = EMPTY;
        handleCaptures(to);
        _turn = _turn.opponent();
        _moveCount++;
        updateWinner();
        positionStack.push(new Board(this));
        positionHistory.add(new Board(this));
    }

    /**
     * Move FROM-TO, but ONLY FOR AI testing purposes.
     * This DOES NOT update stack or set.
     **/
    void testMove(Square from, Square to) {
        assert isLegal(from, to);
        put(get(from), to);
        _pieceState[from.col()][from.row()] = EMPTY;
        handleCaptures(to);
        _turn = _turn.opponent();
        _moveCount++;
        updateWinner();
    }


    /** Helper method called ONLY after a piece moves to Square MOVEDTO.
     * Looks for capturable pieces, and performs necessary tests.
     * Does nothing if no enemy pieces surrounding MOVEDTO.
     * Handles king capture as well.
     * @param movedTo Square piece moved to.
     */
    void handleCaptures(Square movedTo) {
        boolean canCaptureKing = false;
        for (Square neighbor : movedTo.neighbors()) {
            Piece enemy = get(neighbor);
            if (enemy == _turn.opponent()
                    || (_turn == BLACK && enemy == KING)) {
                if (enemy == KING
                        && Arrays.asList(THRONES).contains(neighbor)) {
                    for (Square kingNeighbor : neighbor.neighbors()) {
                        if (get(kingNeighbor) != BLACK
                                && kingNeighbor != THRONE) {
                            canCaptureKing = false;
                            break;
                        }
                        canCaptureKing = true;
                    }
                    if (canCaptureKing) {
                        _pieceState[kingPosition().col()]
                                [kingPosition().row()] = EMPTY;
                    }
                } else {
                    Square capEnd = movedTo.orthogonalEnd(neighbor);
                    if (capEnd != null && isHostile(capEnd)) {
                        capture(movedTo, capEnd);
                    }
                }
            }
        }
    }

    /** Return whether a square is hostile or not to _turn.opposite(),
     * assuming _turn is the player who just made the move.
     * Key edge case: OCCUPIED THRONE (e4) is hostile TO WHITE iff 3
     * BLACK pieces surround it.
     * Another edge case: An UNOCCUPIED THRONE is hostile to BOTH pieces.
     * @param s Square to check hostility
     * @return whether S is hostile or not.**/
    private boolean isHostile(Square s) {
        if (s == THRONE && get(s) == KING && _turn == BLACK) {
            int numBlacksSurrounding = 0;
            for (Square throneNeighbors : s.neighbors()) {
                if (get(throneNeighbors) == BLACK) {
                    numBlacksSurrounding += 1;
                }
            }
            if (numBlacksSurrounding == 3) {
                return true;
            }
        } else if (s == THRONE && get(s) == EMPTY) {
            return true;
        } else if (get(s) == _turn
                || _turn == WHITE && get(s) == KING) {
            return true;
        }
        return false;
    }

    /** Called after each move. Winner of game is updated. **/
    void updateWinner() {
        if (kingPosition() == null) {
            _winner = BLACK;
        } else if (kingPosition().onEdge()) {
            _winner = WHITE;
        } else if (!hasMove(_turn)) {
            _winner = _turn.opponent();
        }
        checkRepeated();
        checkMoves();
    }

    /**
     * Move according to MOVE, assuming it is a legal move.
     * @param move move to make.
     */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /**
     * TEST Move according to move, assuming legal.
     * @param move move to test
     **/
    void testMove(Move move) {
        testMove(move.from(), move.to());
    }

    /**
     * Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     * SQ0 and the necessary conditions are satisfied.
     */
    private void capture(Square sq0, Square sq2) {
        Square toCapture = sq0.between(sq2);
        Piece capturedPiece = get(toCapture);
        _pieceState[toCapture.col()][toCapture.row()] = EMPTY;
        if (capturedPiece == KING) {
            this._winner = _turn;
        }
    }

    /**
     * Undo one move.  Has no effect on the initial board.
     */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
        }
    }


    /**
     * Return number of moves SIDE has made this game.
     * Assumes Black always makes the first move.
     * @return number of moves made by SIDE this game
     **/
    private int movesMade(Piece side) {
        if (side == BLACK) {
            return (int) Math.ceil((_moveCount / 2.0));
        } else if (side == WHITE) {
            return _moveCount - movesMade(BLACK);
        }
        return 0;
    }

    /**
     * Remove record of current position in the set of positions encountered,
     * unless it is a repeated position or we are at the first move.
     * NOTE: Calling undo on the command line calls this twice (AI)
     */
    private void undoPosition() {
        positionStack.pop();
        positionHistory.remove(new Board(this));
        copy(positionStack.peek());
    }

    /**
     * Clear the undo stack and board-position counts. Does not modify the
     * current position or win status.
     */
    void clearUndo() {
        positionStack.clear();
    }

    /**
     * Return a new mutable list of all legal moves on the current board for
     * SIDE (ignoring whose turn it is at the moment).
     */
    List<Move> legalMoves(Piece side) {
        ArrayList<Move> allLegals = new ArrayList<>();
        for (Square pieceSqr : pieceLocations(side)) {
            allLegals.addAll(legalMovesFromSquare(pieceSqr));
        }
        return allLegals;
    }

    /**
     * Helper function that returns an arraylist
     * of ALL legal (rook) moves from a
     * given square, whether black or white.
     * @param pieceSquare square which should contain a black or white piece.
     * @return List of LEGAL moves for the piece at PIECESQUARE.
     */
    ArrayList<Move> legalMovesFromSquare(Square pieceSquare) {
        ArrayList<Move> sqrLegals = new ArrayList<>();
        ArrayList<Square> hittableSqrs = new ArrayList<>();
        hittableSqrs.addAll(allLegalSquaresInDir(pieceSquare, 0));
        hittableSqrs.addAll(allLegalSquaresInDir(pieceSquare, 1));
        hittableSqrs.addAll(allLegalSquaresInDir(pieceSquare, 2));
        hittableSqrs.addAll(allLegalSquaresInDir(pieceSquare, 3));
        for (Square dest : hittableSqrs) {
            sqrLegals.add(mv(pieceSquare, dest));
        }
        return sqrLegals;
    }

    /**
     * Return an arrayList of all (unblocked) Squares
     * from current square in direction DIR. Does not include square S.
     * Throne squares are ignored if the piece is not a king.
     * DIR = 0 for north, 1 for east, 2 for south and 3 for west.
     */
    ArrayList<Square> allLegalSquaresInDir(Square s, int dir) {
        assert (dir == 0 || dir == 1 || dir == 2 || dir == 3);
        int startIndex = (dir == 1 || dir == 3) ? s.col() : s.row();
        int endIndex = (dir == 0 || dir == 1) ? BOARD_SIZE - 1 : 0;
        ArrayList<Square> squares = new ArrayList<>();
        int numStepsBound = Math.abs(startIndex - endIndex);
        for (int steps = 1; steps <= numStepsBound; steps++) {
            if (s.rookMove(dir, steps) == THRONE) {
                if (get(s.rookMove(dir, steps)) != EMPTY) {
                    break;
                } else if (get(s) != KING) {
                    continue;
                }
            }
            if (get(s.rookMove(dir, steps)) == Piece.EMPTY) {
                squares.add(s.rookMove(dir, steps));
            } else {
                break;
            }
        }
        return squares;
    }


    /**
     * Return true iff SIDE has a legal move.
     */
    boolean hasMove(Piece side) {
        if (legalMoves(side).size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Return a text representation of this Board.  If COORDINATES, then row
     * and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /**
     * Return the locations of all pieces on SIDE.
     */
    private HashSet<Square> pieceLocations(Piece side) {
        HashSet<Square> locations = new HashSet<>();
        assert side != EMPTY;
        for (Square s : SQUARE_LIST) {
            if (get(s) == side || get(s) == KING && side == WHITE) {
                locations.add(s);
            }
        }
        return locations;
    }

    /**
     * Return the contents of _board in the order of SQUARE_LIST as a sequence
     * of characters: the toString values of the current turn and Pieces.
     */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /**
     * Return number of pieces that SIDE has on the Board.
     * King is treated as White.
     */
    public int numPieces(Piece side) {
        int num = 0;
        Piece[] board1d = piecesTo1D(this._pieceState);
        for (int i = 0; i < board1d.length; i++) {
            if (board1d[i] == side || (side == WHITE && board1d[i] == KING)) {
                num += 1;
            }
        }
        return num;
    }

    /**
     * Helper method to convert a 2D Piece array to 1D array.
     * @param pieces convert pieceState to 1D array.
     * @return 1d array form of PIECES.
     **/
    public Piece[] piecesTo1D(Piece[][] pieces) {
        List<Piece> list = new ArrayList<Piece>();
        for (int i = 0; i < pieces.length; i++) {
            for (int j = 0; j < pieces[i].length; j++) {
                list.add(pieces[i][j]);
            }
        }
        Piece[] arr1d = new Piece[list.size()];
        for (int i = 0; i < arr1d.length; i++) {
            arr1d[i] = list.get(i);
        }
        return arr1d;
    }

    @Override
    public boolean equals(Object o) {
        Board b = (Board) o;
        if (b == this) {
            return true;
        }
        return comparePositions(b);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_pieceState);
    }

    /** Compare positions.
     * @param b board to compare against current
     * @return whether this board and B are equal.
     */
    public boolean comparePositions(Board b) {
        return Arrays.deepEquals(_pieceState, b._pieceState);
    }

    /**
     * Piece whose turn it is (WHITE or BLACK).
     */
    private Piece _turn;
    /**
     * Cached value of winner on this board, or null if it has not been
     * computed.
     */
    private Piece _winner;
    /**
     * Number of (still undone) moves since initial position.
     */
    private int _moveCount;
    /**
     * True when current board is a repeated position (ending the game).
     */
    private boolean _repeated;
    /**
     * Column-indexed 2D array representing pieces on the board.
     * A null-array element represents an unoccupied square.
     */
    private Piece[][] _pieceState;
    /**
     * 1D array version of pieceState.
     **/
    private Piece[] _piecesArr;
    /**
     * Limit on number of moves.
     **/
    private static int lim;

    /** Return Board's PositionHistory Set. */
    public HashSet<Board> getPositionHistory() {
        return positionHistory;
    }

    /** SET Board's PositionHistory Set.
     * @param posHist position History set.*/
    public void setPositionHistory(HashSet<Board> posHist) {
        this.positionHistory = posHist;
    }

    /**
     * STATIC Set of positions reached so far in the game.
     * Used for previous position checks. Note that every time a move
     * is called on a board, our stack and set is updated.
     */
    private HashSet<Board> positionHistory = new HashSet<>();

    /** Get Board's stack.
     * @return Board's position stack.*/
    public Stack<Board> getPositionStack() {
        return positionStack;
    }

    /** Set Board's stack.
     * @param posStack position stack.*/
    public void setPositionStack(Stack<Board> posStack) {
        positionStack = posStack;
    }

    /**
     * STATIC stack of positions used for UNDO. This means we can only control
     * one game at a time.
     **/
    private Stack<Board> positionStack = new Stack<>();

}
