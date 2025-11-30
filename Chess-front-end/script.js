let legalMovesMap = {}; // To store legal moves from the backend
let promotionPending = null;
var board = null; // Will be initialized later

function makeMove(from, to, promotion = null) {
  const fromX = from[0];
  const fromY = from[1];
  const toX = to[0];
  const toY = to[1];

  let url = `http://localhost:8080/chess/move?fromX=${fromX}&fromY=${fromY}&toX=${toX}&toY=${toY}`;
  if (promotion) {
    url += `&promotion=${promotion}`;
  }

  console.log('Sending move to backend via URL:', url);

  fetch(url)
    .then(response => {
      if (!response.ok) {
        throw new Error(`Failed to send move: ${response.statusText}`);
      }
      return response.json();
    })
    .then(data => {
      console.log('Move result:', data);
      board.position(data); // <--- This works perfectly with the Piece Map!
      fetchAllLegalMoves();

      // --- INTEGRATION: Trigger AI move after a successful human move ---
      if (!data.error) {
          // If the move was successful, it's now the AI's turn
          triggerAiMove();
      }
      // -----------------------------------------------------------------
    })
    .catch(error => {
      console.error('Move error:', error);
      fetchBoard(); // Fallback to ensure the board is in a consistent state
    });
}


// --- NEW FUNCTION TO CALL AI ENDPOINT ---
function triggerAiMove() {
  console.log("AI is calculating its move...");
  fetch('http://localhost:8080/chess/aiMove')
    .then(res => {
      if (!res.ok) {
        return res.text().then(text => {
          throw new Error(`AI Endpoint returned an error: ${text}`);
        });
      }
      // Try to parse as JSON, if it fails, it might be a "Game over" string
      return res.json().catch(() => res.text());
    })
    .then(data => {
      if (typeof data === 'string') {
        console.log(data); // "Game over" message
      } else if (data) {
        console.log("AI move successful. New board state received.");
        board.position(data);
      }
      fetchAllLegalMoves();
    })
    .catch(error => {
      console.error("Error making AI move:", error);
      fetchBoard();
    });
}


// Convert square notation (e.g., 'a1', 'h8') to row/column indices
// 'a1' → [0, 0], 'h8' → [7, 7]
function squareToCoords(square) {
    const file = square.charAt(0); // 'a'–'h'
    const rank = parseInt(square.charAt(1)); // '1'–'8'

    const col = file.charCodeAt(0) - 'a'.charCodeAt(0); // 'a' → 0
    const row = rank - 1; // '1' → 0, '8' → 7

    return [row, col];
  }

  // [0, 0] → 'a1', [7, 7] → 'h8'
  function coordsToSquare(row, col) {
    const file = String.fromCharCode('a'.charCodeAt(0) + col); // 0 → 'a'
    const rank = row + 1; // 0 → '1'

    return file + rank;
  }


// Fetch all legal moves from the backend and store them in a global map
function fetchAllLegalMoves() {
    fetch('http://localhost:8080/chess/legalMoves')
      .then(res => res.json())
      .then(data => {
        console.log("Fetched raw legalMoves from backend:", data);

        const normalized = {};
        for (const key in data) {
          if (!key.includes("x=") || !key.includes("y=")) {
            continue; 
          }

          const match = key.match(/x=(\d+),\s*y=(\d+)/);
          if (match) {
            const row = parseInt(match[1]);
            const col = parseInt(match[2]);
            normalized[`${row},${col}`] = data[key];
          }
        }

        console.log("Normalized legalMovesMap:", normalized);
        legalMovesMap = normalized;
        
        // --- NEW LINE ADDED HERE ---
        highlightAllLegalMoves(); 
        // ---------------------------
      })
      .catch(err => console.error("Failed to fetch legal moves", err));
}




// Handler for when a piece is being dragged
function onDragStartHandler(source, piece, position, orientation) {
    console.log("onDragStartHandler triggered!", source, piece);
  const [row, col] = squareToCoords(source);
  const key = `${row},${col}`;

  // Get the legal moves for the piece being dragged
  const legal = legalMovesMap[key] || [];
  // No highlighting needed, we're just restricting the moves in onDropHandler
}

// Handler for when a piece is dropped on the board
function onDropHandler(source, target) {
    console.log("onDropHandler triggered!", source, "→", target);

    const fromCoords = squareToCoords(source);
    const toCoords = squareToCoords(target);

    const key = `${fromCoords[0]},${fromCoords[1]}`;
    const legal = legalMovesMap[key] || [];

    console.log("From coords:", fromCoords);
    console.log("To coords:", toCoords);
    console.log("Key used:", key);
    console.log("Legal moves for this key:", legal);

    const isValid = legal.some(move => move.x === toCoords[0] && move.y === toCoords[1]);
    console.log("Is move valid?", isValid);

    if (!isValid) {
      console.log("Move rejected. Snapping back.");
      return 'snapback';
    }

    const piece = board.position()[source];
    const isWhitePawn = piece === 'wP' && toCoords[0] === 7;
    const isBlackPawn = piece === 'bP' && toCoords[0] === 0;

    if (isWhitePawn || isBlackPawn) {
      promotionPending = { fromCoords, toCoords };
      const color = piece.charAt(0); // 'w' or 'b'
      showPromotionModal(color);
      return;
    }

    makeMove(fromCoords, toCoords);
  }


// Initialize the chessboard
board = Chessboard('board', {
  position: 'start',  // Position set to 'start' to initiate the game from the beginning
  draggable: true,
  orientation: 'white',
  pieceTheme: 'img/chesspieces/wikipedia/{piece}.png',
  onDrop: onDropHandler,
  onDragStart: onDragStartHandler,
});

document.addEventListener('keydown', function(event) {
  if (event.key === 'f' || event.key === 'F') {
    board.flip();
  }
});

// Call fetchAllLegalMoves to get legal moves when the game starts or after every move
fetchAllLegalMoves();

function flipBoard(event) {
   board.flip;
}

function showPromotionModal(color) {
    const modal = document.getElementById('promotionModal');
    modal.innerHTML = `
      <p>Choose promotion piece:</p>
      <img src="img/chesspieces/wikipedia/${color}Q.png" onclick="selectPromotion('q')" />
      <img src="img/chesspieces/wikipedia/${color}R.png" onclick="selectPromotion('r')" />
      <img src="img/chesspieces/wikipedia/${color}B.png" onclick="selectPromotion('b')" />
      <img src="img/chesspieces/wikipedia/${color}N.png" onclick="selectPromotion('n')" />
    `;
    modal.style.display = 'block';
  }

  function selectPromotion(promotionType) {
    const { fromCoords, toCoords } = promotionPending;
    promotionPending = null;

    document.getElementById('promotionModal').style.display = 'none';

    makeMove(fromCoords, toCoords, promotionType);
  }

function fetchBoard() {
    // Use the specific FEN endpoint we created for the Bitboard engine
    fetch('http://localhost:8080/chess/fen')
        .then(res => res.text()) // Get raw string, not JSON
        .then(fenString => {
            console.log("Synced Board FEN:", fenString);
            board.position(fenString);
        })
        .catch(err => console.error("Error fetching board:", err));
}

// Start the game by fetching the initial board state and legal moves
fetch('http://localhost:8080/chess/start', { method: 'POST' })
  .then(() => {
      fetchBoard();
      fetchAllLegalMoves();
  });

  // NEW: Function to highlight all squares that are legal destinations
function highlightAllLegalMoves() {
  // 1. Clear previous highlights
  // chessboard.js uses the class 'square-55d63' for all squares.
  $('.square-55d63').removeClass('legal-move');

  // 2. Iterate over the global legalMovesMap
  // format: "row,col": [ {x: 2, y: 3}, ... ]
  for (const startSquareKey in legalMovesMap) {
    if (legalMovesMap.hasOwnProperty(startSquareKey)) {
      const moves = legalMovesMap[startSquareKey];
      
      // 3. Loop through every target move for this piece
      moves.forEach(move => {
        // Convert the backend coordinates (x,y) to 'a1', 'e4' notation
        const squareId = coordsToSquare(move.x, move.y);
        
        // 4. Find the square using jQuery and add the class
        // chessboard.js adds classes like 'square-a1', 'square-e4' to the divs
        $('.square-' + squareId).addClass('legal-move');
      });
    }
  }
}