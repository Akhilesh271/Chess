let legalMovesMap = {}; 
let promotionPending = null;
var board = null; 
let playerSide = 'white'; 

const BACKEND_URL = "https://chess-2u5e.onrender.com";

function startGame(side) {
    playerSide = side;
    document.getElementById('startModal').style.display = 'none'; 

    board = Chessboard('board', {
        position: 'start',
        draggable: true,
        orientation: side, 
        pieceTheme: 'img/chesspieces/wikipedia/{piece}.png',
        onDrop: onDropHandler,
        onDragStart: onDragStartHandler,
    });
    
    board.resize();

    fetch(BACKEND_URL + '/chess/start', { method: 'POST' })
        .then(() => {
            updateStatus("Game Started. White to move.");
            fetchBoard();       
            fetchAllLegalMoves(); 

            if (playerSide === 'black') {
                updateStatus("AI (White) is thinking...");
                setTimeout(triggerAiMove, 500); 
            }
        })
        .catch(err => console.error("Error starting game:", err));
}

function makeMove(from, to, promotion = null) {
    const fromX = from[0];
    const fromY = from[1];
    const toX = to[0];
    const toY = to[1];

    let url = `${BACKEND_URL}/chess/move?fromX=${fromX}&fromY=${fromY}&toX=${toX}&toY=${toY}`;
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
            board.position(data); 
            fetchAllLegalMoves(); 
            triggerAiMove();
        })
        .catch(error => {
            console.error('Move error:', error);
            fetchBoard(); 
        });
}

function triggerAiMove() {
    updateStatus("AI is thinking...");
    
    fetch(BACKEND_URL + '/chess/aiMove')
        .then(res => {
            if (!res.ok) throw new Error('AI Error');
            return res.text().then(text => {
                try {
                    return JSON.parse(text);
                } catch {
                    return text; 
                }
            });
        })
        .then(data => {
            if (typeof data === 'string') {
                updateStatus(data); 
            } else {
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
    if (!board) return false;
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
    fetch(BACKEND_URL + '/chess/legalMoves')
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
        // highlightAllLegalMoves();
      })
      .catch(err => console.error(err));
}

function highlightAllLegalMoves() {
  $('.square-55d63').removeClass('legal-move-indicator');
  for (const startSquareKey in legalMovesMap) {
    const moves = legalMovesMap[startSquareKey];
    moves.forEach(move => {
        const squareId = coordsToSquare(move.x, move.y);
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
    fetch(BACKEND_URL + '/chess/fen')
        .then(res => res.text())
        .then(fen => board.position(fen))
        .catch(err => console.error(err));
}