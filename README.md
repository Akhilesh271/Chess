# Java Spring Boot Chess Engine

A full-stack chess application featuring a custom-built AI, move validation, and a high-performance bitboard engine. The project demonstrates advanced Java concepts, algorithm optimization (Minimax with Alpha-Beta Pruning), and full-stack deployment using Docker and Render.

[Live Demo](https://chess-2u5e.onrender.com)

# Gameplay Image
<img width="895" height="535" alt="Screenshot 2025-12-17 at 16 37 16" src="https://github.com/user-attachments/assets/2ee92aa0-cb4d-48ae-8d8c-78b6b73effa6" />


## Features

* **Dual Engine Architecture:**
    * **OOP Engine:** A legacy object-oriented implementation for readability and logic prototyping.
    * **Bitboard Engine:** A high-performance implementation using 64-bit integers (longs) for board representation, offering significantly faster move generation and AI calculation.
* **Chess AI:** Implements the **Minimax algorithm** with **Alpha-Beta pruning** to calculate optimal moves.
* **Move Validation:** Full enforcement of standard chess rules (castling, en passant, promotions, pins).
* **Interactive UI:**
    * Drag-and-drop & Click-to-move support.
    * Legal move highlighting.
    * Responsive design (Mobile & Desktop friendly).
* **Deployment:** Containerized with **Docker** and hosted on **Render**.

## Tech Stack

* **Backend:** Java 21, Spring Boot, Maven
* **Frontend:** HTML5, CSS3, JavaScript, jQuery, Chessboard.js
* **DevOps:** Docker, Render
* **Algorithms:** Minimax, Alpha-Beta Pruning, Bitwise Operations

## Architecture: OOP vs. Bitboards

This project includes two distinct implementations of the chess rules to demonstrate performance optimization:

1.  **Object-Oriented (OOP):** Uses a 2D array of `Piece` objects. While intuitive, it is computationally expensive for deep AI searches due to memory overhead and object references.
2.  **Bitboards (Optimized):** Uses 64-bit integers to represent the board state. This allows the engine to calculate moves using bitwise operators (`&`, `|`, `^`, `<<`, `>>`), which are processed directly by the CPU. This optimization allows the AI to search much deeper in the same amount of time.

*You can switch engines in the Spring Boot configuration using `@Profile("bitboard")` or `@Profile("oop")`.*

## Getting Started

### Prerequisites
* Java 21 SDK
* Maven

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/Akhilesh271/Chess.git](https://github.com/Akhilesh271/Chess.git)
    cd Chess
    ```

2.  **Navigate to the backend:**
    ```bash
    cd Chess-back-end
    ```

3.  **Build the project:**
    ```bash
    mvn clean package
    ```

4.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```

5.  **Play:**
    Open your browser and navigate to `http://localhost:8080`.

## Docker Deployment

The application is containerized for easy deployment.

```dockerfile
# Build
docker build -t chess-app .

# Run
docker run -p 10000:10000 chess-app
