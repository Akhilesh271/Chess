const boardElement = document.getElementById("board");

let selectedSquare = null;
let boardState = [];

function fetchBoard() {
  fetch('http://localhost:8080/chess/board')
    .then(res => res.json())
    .then(data => {
      boardState = data.board;
      renderBoard();
    });
}

function renderBoard() {
  boardElement.innerHTML = "";
  for (let i = 0; i < 8; i++) {
    for (let j = 0; j < 8; j++) {
      const square = document.createElement("div");
      square.classList.add("square", (i + j) % 2 === 0 ? "white" : "black");
      const piece = boardState[i][j];
      if (piece) {
        square.textContent = piece.symbol || piece.type[0]; // customize
      }
      square.dataset.row = i;
      square.dataset.col = j;
      square.addEventListener("click", handleSquareClick);
      boardElement.appendChild(square);
    }
  }
}

function handleSquareClick(e) {
  const row = parseInt(e.target.dataset.row);
  const col = parseInt(e.target.dataset.col);

  if (!selectedSquare) {
    selectedSquare = { row, col };
    highlightLegalMoves(row, col);
  } else {
    makeMove(selectedSquare, { row, col });
    selectedSquare = null;
  }
}

function highlightLegalMoves(row, col) {
  fetch(`http://localhost:8080/chess/legalMoves/${row}/${col}`)
    .then(res => res.json())
    .then(data => {
      data.forEach(pair => {
        const square = document.querySelector(`[data-row="${pair.x}"][data-col="${pair.y}"]`);
        if (square) square.classList.add("highlight");
      });
    });
}

function makeMove(from, to) {
  fetch('http://localhost:8080/chess/move', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ from, to })
  })
    .then(res => res.text())
    .then(msg => {
      console.log(msg);
      fetchBoard(); // reload the board
    });
}

// Start the game
fetch('http://localhost:8080/chess/start', { method: 'POST' })
  .then(() => fetchBoard());