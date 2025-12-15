let legalMovesMap = {};
let promotionPending = null;
var board = null;
let playerSide = 'white';


function startGame(side) {
    playerSide = side;
    document.getElementById('startModal').style.display = 'none';

    // Initialize board with the chosen orientation
    board = Chessboard('board', {
        position: 'start',
        draggable: true,
        orientation: side, // 'white' or 'black'
        pieceTheme: 'img/chesspieces/wikipedia/{piece}.png',
        onDrop: onDropHandler,
        onDragStart: onDragStartHandler,
    });

    // Start the backend session (Using your Render URL)
    fetch('https://chess-2u5e.onrender.com/chess/start', { method: 'POST' })
        .then(() => {
            updateStatus("Game Started. White to move.");
            fetchBoard();         // Sync board
            fetchAllLegalMoves(); // Get moves

            // If user is playing Black, AI (White) must move first immediately
            if (playerSide === 'black') {
                updateStatus("AI (White) is thinking...");
                setTimeout(triggerAiMove, 500); // Small delay
            }
        })
        .catch(err => console.error("Error starting game:", err));
}


function makeMove(from, to, promotion = null) {
    const fromX = from[0];
    const fromY = from[1];
    const toX = to[0];
    const toY = to[1];
    let url = `https://chess-2u5e.onrender.com/chess/move?fromX=${fromX}&fromY=${fromY}&toX=${toX}&toY=${toY}`;
    if (promotion) {
        url += `&promotion=${promotion}`;
    }

    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Move failed');
            return response.json();
        })
        .then(data => {
            if (data.error) {
                console.error("Invalid move:", data.error);
                return;
            }
            
            // 1. Update Board
            board.position(data);
            
            // 2. Refresh Legal Moves for the next turn
            fetchAllLegalMoves();

            // 3. Trigger AI if the game isn't over
            triggerAiMove();
        })
        .catch(error => {
            console.error('Move error:', error);
            fetchBoard(); // Fallback sync
        });
}

function triggerAiMove() {
    updateStatus("AI is thinking...");
    
    // UPDATED URL
    fetch('https://chess-2u5e.onrender.com/chess/aiMove')
        .then(res => {
            if (!res.ok) throw new Error('AI Error');
            // Handle plain text "Game Over" or JSON
            return res.text().then(text => {
                try {
                    return JSON.parse(text);
                } catch {
                    return text; // It's likely a "Game Over" string
                }
            });
        })
        .then(data => {
            if (typeof data === 'string') {
                updateStatus(data); // "Checkmate" or "Draw"
            } else {
                // AI moved successfully
                board.position(data);
                fetchAllLegalMoves();
                updateStatus("Your Turn");
            }
        })
        .catch(error => {
            console.error("AI Move failed:", error);
        });
}


function onDragStartHandler(source, piece) {
    // Prevent moving if game hasn't started
    if (!board) return false;

    // Prevent moving opponent's pieces
    if (playerSide === 'white' && piece.search(/^b/) !== -1) return false;
    if (playerSide === 'black' && piece.search(/^w/) !== -1) return false;
}

function onDropHandler(source, target) {
    const fromCoords = squareToCoords(source);
    const toCoords = squareToCoords(target);
    const key = `${fromCoords[0]},${fromCoords[1]}`;
    
    const legal = legalMovesMap[key] || [];
    const isValid = legal.some(move => move.x === toCoords[0] && move.y === toCoords[1]);

    if (!isValid) return 'snapback';

    // Promotion Logic
    const piece = board.position()[source];
    const isWhitePawn = piece === 'wP' && toCoords[0] === 7;
    const isBlackPawn = piece === 'bP' && toCoords[0] === 0;

    if (isWhitePawn || isBlackPawn) {
        promotionPending = { fromCoords, toCoords };
        const color = piece.charAt(0); 
        showPromotionModal(color);
        return;
    }

    makeMove(fromCoords, toCoords);
}


function fetchAllLegalMoves() {
    fetch('https://chess-2u5e.onrender.com/chess/legalMoves')
      .then(res => res.json())
      .then(data => {
        const normalized = {};
        for (const key in data) {
          if (!key.includes("x=") || !key.includes("y=")) continue;
          const match = key.match(/x=(\d+),\s*y=(\d+)/);
          if (match) {
            const row = parseInt(match[1]);
            const col = parseInt(match[2]);
            normalized[`${row},${col}`] = data[key];
          }
        }
        legalMovesMap = normalized;
        highlightAllLegalMoves();
      })
      .catch(err => console.error(err));
}

// Highlight squares (using the CSS class defined in style.css)
function highlightAllLegalMoves() {
  $('.square-55d63').removeClass('legal-move-indicator');

  for (const startSquareKey in legalMovesMap) {
    const moves = legalMovesMap[startSquareKey];
    moves.forEach(move => {
        const squareId = coordsToSquare(move.x, move.y);
        // Add specific class for CSS styling
        $('.square-' + squareId).addClass('legal-move-indicator');
    });
  }
}

function updateStatus(text) {
    const el = document.getElementById('status');
    if(el) el.innerText = text;
}

function showPromotionModal(color) {
    const modal = document.getElementById('promotionModal');
    // Get images from chessboard.js
    modal.innerHTML = `
      <p>Promote to:</p>
      <div style="display:flex; gap:10px; justify-content:center;">
          <img src="img/chesspieces/wikipedia/${color}Q.png" onclick="selectPromotion('q')" />
          <img src="img/chesspieces/wikipedia/${color}R.png" onclick="selectPromotion('r')" />
          <img src="img/chesspieces/wikipedia/${color}B.png" onclick="selectPromotion('b')" />
          <img src="img/chesspieces/wikipedia/${color}N.png" onclick="selectPromotion('n')" />
      </div>
    `;
    modal.style.display = 'block';
}

function selectPromotion(type) {
    const { fromCoords, toCoords } = promotionPending;
    promotionPending = null;
    document.getElementById('promotionModal').style.display = 'none';
    makeMove(fromCoords, toCoords, type);
}

// Coordinate Converters
function squareToCoords(square) {
    const file = square.charAt(0);
    const rank = parseInt(square.charAt(1));
    const col = file.charCodeAt(0) - 'a'.charCodeAt(0);
    const row = rank - 1;
    return [row, col];
}

function coordsToSquare(row, col) {
    const file = String.fromCharCode('a'.charCodeAt(0) + col);
    const rank = row + 1;
    return file + rank;
}

function fetchBoard() {
    fetch('https://chess-2u5e.onrender.com/chess/fen')
        .then(res => res.text())
        .then(fen => board.position(fen))
        .catch(err => console.error(err));
}

// Keep board responsive
if (window.board) {
    window.addEventListener('resize', board.resize);
}