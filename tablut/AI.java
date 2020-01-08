package tablut;

import java.util.ArrayList;

/**
 * A Player that automatically generates moves.
 *
 * @author Kevin Moy
 */
class AI extends Player {

    /**
     * A position-score magnitude indicating a win (for white if positive,
     * black if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /**
     * A position-score magnitude indicating a forced win in a subsequent
     * move.  This differs from WINNING_VALUE to avoid putting off wins.
     */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new AI with no piece or controller (intended to produce
     * a template).
     */
    AI() {
        this(null, null);
    }

    /**
     * A new AI playing PIECE under control of CONTROLLER.
     */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        _lastFoundMove = null;
        if (board().turn() != _myPiece || board().winner() != null) {
            return null;
        }
        Move theMove = findMove();
        _controller.reportMove(theMove);
        return theMove.toString();
    }


    @Override
    boolean isManual() {
        return false;
    }

    /**
     * Select a move for me from the current position, assuming there
     * is a move.
     * Essentially, we iterate through all legal moves and build a list of
     * move values for each move. We shall return the MAXIMUM of these values.
     */
    private Move findMove() {
        Board b = new Board(board(), 1);
        if (myPiece() == Piece.WHITE) {
            evalScore(b, 0, true, 1, -INFTY, INFTY);
        } else {
            evalScore(b, 0, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /**
     * Return index of maximum value in ARR.
     */
    public int maxMoveInd(double[] arr) {
        double currentMax = arr[0];
        int maxIndex = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > currentMax) {
                currentMax = arr[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * Return index of minimum value in ARR.
     */
    public int minMoveInd(double[] arr) {
        double currentMin = arr[0];
        int minIndex = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < currentMin) {
                currentMin = arr[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    /**
     * The move found by the last call to one of the ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /**
     * Evaluate a position's SCORE via minimax algorithm. The position score
     * should have maximal value or have value > BETA if SENSE==1
     * and minimal value or value < ALPHA if SENSE==-1.
     * If sense == 1, we test all white moves and return the best.
     * If sense == -1, we test all BLACK moves and return the best.
     * Searches up to DEPTH levels.  Searching at level 0
     * simply returns a static estimate
     * of the board value and does not set _lastMoveFound.
     * @param board Board.
     * @param depth current depth
     * @param saveMove useless.
     * @param sense indicator for max/min
     * @param alpha best possible score maximizer "sees".
     * @param beta best possible score minimizer "sees"
     * @return score for this position.
     */
    private int evalScore(Board board, int depth, boolean saveMove,
                          int sense, int alpha, int beta) {
        if (board.winner() != null) {
            return winVal(board);
        }
        if (depth == maxDepth(board)) {
            return simpleFindMove(board, saveMove, sense, alpha, beta);
        }
        if (sense == 1) {
            Move bestMove = null;
            int bestScore = -INFTY;
            for (Move move : board.legalMoves(Piece.WHITE)) {
                board.makeMove(move);
                int response = evalScore(board, depth + 1,
                        false, -1, alpha, beta);
                board.undo();
                if (response > bestScore) {
                    bestMove = move;
                    bestScore = response;
                    alpha = Math.max(alpha, response);
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMove;
            }
            return bestScore;
        } else if (sense == -1) {
            Move bestMove = null;
            int bestScore = INFTY;
            for (Move move : board.legalMoves(Piece.BLACK)) {
                board.makeMove(move);
                int response = evalScore(board, depth + 1,
                        false, 1, alpha, beta);
                board.undo();
                if (response < bestScore) {
                    bestMove = move;
                    bestScore = response;
                    beta = Math.min(beta, response);
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMove;
            }
            return bestScore;
        }
        return 0;
    }

    /** Assumes board is a DUB,
     * then return the corresponding winVal.
     * Key:
     * @param board board to look at.
     * */
    private int winVal(Board board) {
        if (board.winner() == Piece.WHITE) {
            return WINNING_VALUE;
        } else {
            return -WINNING_VALUE;
        }
    }


    /** Return the SCORE of the best move for a maximizing player (WHITE).
     * @param board Board.
     * @param depth current depth
     * @param saveMove useless.
     * @param alpha best possible score maximizer "sees"
     * @param beta best possible score minimizer "sees"
     * @return score for this position.
     * */
    private int findMax(Board board, int depth,
                        boolean saveMove, int alpha, int beta) {
        if (depth == maxDepth(board) || board.winner() != null) {
            return simpleFindMin(board, saveMove, alpha, beta);
        }
        Move bestMove = null;
        int bestMoveVal = -INFTY;
        for (Move move : board.legalMoves(Piece.WHITE)) {
            board.makeMove(move);
            int response = findMin(board, depth + 1, false, alpha, beta);
            board.undo();
            if (response >= bestMoveVal) {
                bestMove = move;
                bestMoveVal = response;
                alpha = Math.max(alpha, response);
                if (alpha <= beta) {
                    break;
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = bestMove;
        }
        return bestMoveVal;
    }

    /** Return the SCORE of the best move for a minimizing player.
     * @param board Board.
     * @param depth current depth
     * @param saveMove useless.
     * @param alpha best possible score maximizer "sees"
     * @param beta best possible score minimizer "sees"
     * @return score for this position.
     * */
    private int findMin(Board board, int depth, boolean saveMove,
                        int alpha, int beta) {
        if (depth == maxDepth(board) || board.winner() != null) {
            return simpleFindMin(board, saveMove, alpha, beta);
        }
        Move bestMove = null;
        int bestMoveVal = -INFTY;
        for (Move move : board.legalMoves(Piece.WHITE)) {
            board.makeMove(move);
            int response = findMin(board, depth + 1, false, alpha, beta);
            board.undo();
            if (response >= bestMoveVal) {
                bestMove = move;
                bestMoveVal = response;
                alpha = Math.max(alpha, response);
                if (alpha <= beta) {
                    break;
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = bestMove;
        }
        return bestMoveVal;
    }

    /** Just looks 1 layer deep for WHITE (the maximizer).
     * @param board Board
     * @param saveMove whether to save move or not.
     * @param alpha alpha val
     * @param beta beta val
     * @return simplest move.*/
    private int simpleFindMax(Board board, boolean saveMove,
                              int alpha, int beta) {
        if (board.winner() != null) {
            return winVal(board);
        }
        Move bestMove = null;
        int bestMoveVal = -INFTY;
        for (Move move : board.legalMoves(Piece.WHITE)) {
            board.makeMove(move);
            int moveVal = simpleStaticScore(board);
            board.undo();
            if (moveVal <= bestMoveVal) {
                bestMove = move;
                beta = Math.min(beta, moveVal);
                if (alpha >= beta) {
                    break;
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = bestMove;
        }
        return bestMoveVal;
    }

    /** Just looks 1 layer deep for BLACK (the minimizer).
     * @param board Board
     * @param alpha alpha val
     * @param beta beta val
     * @param saveMove whether/not to save move
     * @return minimum*/
    private int simpleFindMin(Board board, boolean saveMove,
                              int alpha, int beta) {
        if (board.winner() != null) {
            return winVal(board);
        }
        Move bestMove = null;
        int bestMoveVal = INFTY;
        for (Move move : board.legalMoves(Piece.BLACK)) {
            board.makeMove(move);
            int moveVal = simpleStaticScore(board);
            board.undo();
            if (moveVal <= bestMoveVal) {
                bestMove = move;
                beta = Math.min(beta, moveVal);
                if (alpha >= beta) {
                    break;
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = bestMove;
        }
        return bestMoveVal;
    }

    /** Simply look at the next possible move
     * and return a direct heuristic.
     * @param board board
     * @param saveMove whether to save move
     * @param sense max/min indicator
     * @param alpha alpha val
     * @param beta beta val*/
    private int simpleFindMove(Board board, boolean saveMove,
                               int sense, int alpha, int beta) {
        if (board.winner() != null) {
            return winVal(board);
        }
        if (sense == 1) {
            Move bestMove = null;
            int bestScore = -INFTY;
            for (Move move : board.legalMoves(Piece.WHITE)) {
                board.makeMove(move);
                int response = simpleStaticScore(board);
                board.undo();
                if (response > bestScore) {
                    bestMove = move;
                    bestScore = response;
                    alpha = Math.max(alpha, response);
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMove;
            }
            return bestScore;
        } else if (sense == -1) {
            Move bestMove = null;
            int bestScore = INFTY;
            for (Move move : board.legalMoves(Piece.BLACK)) {
                board.makeMove(move);
                int response = simpleStaticScore(board);
                board.undo();
                if (response < bestScore) {
                    bestMove = move;
                    bestScore = response;
                    beta = Math.min(beta, response);
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
            if (saveMove) {
                _lastFoundMove = bestMove;
            }
            return bestScore;
        }
        return 0;
    }

    /**
     * Return an ArrayList of all possible Boards that are BOARD, but
     * one move ahead. This assumes it's my opponent's turn to move.
     **/
    private ArrayList<Board> oneMoveAhead(Board board) {
        ArrayList<Board> boards = new ArrayList<>();
        for (Move oneMove : board.legalMoves(_myPiece.opponent())) {
            Board oneAhead = new Board(board, 1);
            oneAhead.testMove(oneMove);
            boards.add(oneAhead);
        }
        return boards;
    }

    /**
     * Return a heuristically determined maximum search depth
     * based on characteristics of BOARD.
     */
    private static int maxDepth(Board board) {
        return 3;
    }

    /**
     * Return a heuristic value for BOARD.
     * For now, this will be very basic:
     * simply the number of our pieces - opponent's pieces.
     */
    private int simpleStaticScore(Board board) {
        return board.numPieces(_myPiece) - board.numPieces(_myPiece.opponent());
    }

    /** Return a more sophisticated heuristic value for BOARD. **/
    private int staticScore(Board board) {
        return 1;
    }

    /**
     * Time limit of searching algorithm- extremely generous 20 seconds.
     **/
    private static final int TIMELIMIT = 20000;

}
