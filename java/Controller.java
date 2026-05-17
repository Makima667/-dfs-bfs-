import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import javax.imageio.ImageIO;

public class Controller {
    @FXML private Canvas canvas;
    @FXML private ComboBox<String> algorithmBox;
    @FXML private Slider speedSlider;
    @FXML private CheckBox diagonalCheckBox;
    @FXML private Button startButton;
    @FXML private Label statusLabel;
    @FXML private Label visitedLabel;
    @FXML private Label pathLabel;
    @FXML private Label timeLabel;

    private static final int ROWS = 25;
    private static final int COLS = 30;

    private static final int EMPTY = 0;
    private static final int WALL = 1;
    private static final int START = 2;
    private static final int END = 3;
    private static final int VISITED = 4;
    private static final int PATH = 5;

    private final int[][] grid = new int[ROWS][COLS];
    private GridPoint start = new GridPoint(12, 5);
    private GridPoint end = new GridPoint(12, 24);
    private Timeline animation;
    private boolean running = false;
    private Boolean draggingWallMode = null;

    @FXML
    public void initialize() {
        algorithmBox.getItems().addAll("BFS", "DFS", "Dijkstra", "A*");
        algorithmBox.getSelectionModel().select("BFS");

        resetGrid();
        bindMouseEvents();
        drawGrid();
        updateStatus("准备就绪：左键画墙，Shift+左键设起点，Ctrl+左键设终点。", 0, 0, 0);
    }

    @FXML
    private void handleStart() {
        if (running) {
            stopAnimation("已停止。可以清除痕迹后重新运行。");
            return;
        }

        clearSearchMarks();
        String algorithm = algorithmBox.getValue();
        SearchResult result;
        long begin = System.nanoTime();

        switch (algorithm) {
            case "DFS":
                result = runDfs();
                break;
            case "Dijkstra":
                result = runDijkstra();
                break;
            case "A*":
                result = runAStar();
                break;
            case "BFS":
            default:
                result = runBfs();
                break;
        }

        result.timeMs = (System.nanoTime() - begin) / 1_000_000.0;
        animateResult(result, algorithm);
    }

    @FXML
    private void handleClearSearch() {
        stopAnimation(null);
        clearSearchMarks();
        drawGrid();
        updateStatus("已清除搜索痕迹，保留墙、起点和终点。", 0, 0, 0);
    }

    @FXML
    private void handleClearWalls() {
        stopAnimation(null);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == WALL || grid[r][c] == VISITED || grid[r][c] == PATH) {
                    grid[r][c] = EMPTY;
                }
            }
        }
        placeStartAndEnd();
        drawGrid();
        updateStatus("已清空障碍物。", 0, 0, 0);
    }

    @FXML
    private void handleReset() {
        stopAnimation(null);
        resetGrid();
        drawGrid();
        updateStatus("地图已重置。", 0, 0, 0);
    }

    @FXML
    private void handleRandomWalls() {
        stopAnimation(null);
        clearAllExceptStartEnd();
        Random random = new Random();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                GridPoint p = new GridPoint(r, c);
                if (!p.equals(start) && !p.equals(end) && random.nextDouble() < 0.27) {
                    grid[r][c] = WALL;
                }
            }
        }
        placeStartAndEnd();
        drawGrid();
        updateStatus("已随机生成障碍物。", 0, 0, 0);
    }

    @FXML
    private void handleGenerateMaze() {
        stopAnimation(null);
        generateMaze();
        drawGrid();
        updateStatus("已生成迷宫。建议使用 BFS、Dijkstra 或 A* 观察最短路径。", 0, 0, 0);
    }

    @FXML
    private void handleExportImage() {
        stopAnimation(null);

        File screenshotDir = new File(System.getProperty("user.dir"), "screenshots");
        if (!screenshotDir.exists() && !screenshotDir.mkdirs()) {
            updateStatus("截图导出失败：无法创建 screenshots 文件夹。", 0, 0, 0);
            return;
        }

        String fileName = "PathLab_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
        File output = new File(screenshotDir, fileName);

        try {
            WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
            canvas.snapshot(null, image);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", output);
            updateStatus("截图已保存：" + output.getAbsolutePath(), 0, 0, 0);
        } catch (IOException ex) {
            updateStatus("截图导出失败：" + ex.getMessage(), 0, 0, 0);
        }
    }

    private void bindMouseEvents() {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> draggingWallMode = null);
    }

    private void handleMousePressed(MouseEvent event) {
        if (running) return;
        GridPoint p = toGridPoint(event);
        if (!isInside(p.row, p.col)) return;

        clearSearchMarks();

        if (event.getButton() == MouseButton.PRIMARY && event.isShiftDown()) {
            setStart(p);
        } else if (event.getButton() == MouseButton.PRIMARY && event.isControlDown()) {
            setEnd(p);
        } else if (event.getButton() == MouseButton.SECONDARY) {
            clearCell(p);
            draggingWallMode = false;
        } else if (event.getButton() == MouseButton.PRIMARY) {
            if (!p.equals(start) && !p.equals(end)) {
                draggingWallMode = grid[p.row][p.col] != WALL;
                grid[p.row][p.col] = draggingWallMode ? WALL : EMPTY;
            }
        }

        placeStartAndEnd();
        drawGrid();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (running || draggingWallMode == null) return;
        GridPoint p = toGridPoint(event);
        if (!isInside(p.row, p.col) || p.equals(start) || p.equals(end)) return;
        grid[p.row][p.col] = draggingWallMode ? WALL : EMPTY;
        drawGrid();
    }

    private void setStart(GridPoint p) {
        if (p.equals(end)) return;
        grid[start.row][start.col] = EMPTY;
        start = p;
        grid[start.row][start.col] = START;
    }

    private void setEnd(GridPoint p) {
        if (p.equals(start)) return;
        grid[end.row][end.col] = EMPTY;
        end = p;
        grid[end.row][end.col] = END;
    }

    private void clearCell(GridPoint p) {
        if (p.equals(start) || p.equals(end)) return;
        grid[p.row][p.col] = EMPTY;
    }

    private GridPoint toGridPoint(MouseEvent event) {
        int col = (int) (event.getX() / cellWidth());
        int row = (int) (event.getY() / cellHeight());
        return new GridPoint(row, col);
    }

    private void resetGrid() {
        for (int[] row : grid) Arrays.fill(row, EMPTY);
        start = new GridPoint(12, 5);
        end = new GridPoint(12, 24);
        placeStartAndEnd();
    }

    private void clearSearchMarks() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == VISITED || grid[r][c] == PATH) {
                    grid[r][c] = EMPTY;
                }
            }
        }
        placeStartAndEnd();
    }

    private void clearAllExceptStartEnd() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = EMPTY;
            }
        }
        placeStartAndEnd();
    }

    private void generateMaze() {
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(grid[r], WALL);
        }

        Random random = new Random();
        ArrayDeque<GridPoint> stack = new ArrayDeque<>();
        GridPoint mazeStart = new GridPoint(1, 1);
        grid[mazeStart.row][mazeStart.col] = EMPTY;
        stack.push(mazeStart);

        int[][] directions = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};
        while (!stack.isEmpty()) {
            GridPoint current = stack.peek();
            List<GridPoint> candidates = new ArrayList<>();

            for (int[] d : directions) {
                int nr = current.row + d[0];
                int nc = current.col + d[1];
                if (nr > 0 && nr < ROWS - 1 && nc > 0 && nc < COLS - 1 && grid[nr][nc] == WALL) {
                    candidates.add(new GridPoint(nr, nc));
                }
            }

            if (candidates.isEmpty()) {
                stack.pop();
                continue;
            }

            GridPoint next = candidates.get(random.nextInt(candidates.size()));
            int wallRow = (current.row + next.row) / 2;
            int wallCol = (current.col + next.col) / 2;
            grid[wallRow][wallCol] = EMPTY;
            grid[next.row][next.col] = EMPTY;
            stack.push(next);
        }

        start = new GridPoint(1, 1);
        end = new GridPoint(ROWS - 2, COLS - 3);
        grid[start.row][start.col] = EMPTY;
        grid[end.row][end.col] = EMPTY;
        placeStartAndEnd();
    }

    private void placeStartAndEnd() {
        grid[start.row][start.col] = START;
        grid[end.row][end.col] = END;
    }

    private SearchResult runBfs() {
        SearchResult result = new SearchResult();
        boolean[][] seen = new boolean[ROWS][COLS];
        GridPoint[][] parent = new GridPoint[ROWS][COLS];
        ArrayDeque<GridPoint> queue = new ArrayDeque<>();

        queue.offer(start);
        seen[start.row][start.col] = true;

        while (!queue.isEmpty()) {
            GridPoint cur = queue.poll();
            if (!cur.equals(start)) result.visitedOrder.add(cur);
            if (cur.equals(end)) {
                result.found = true;
                break;
            }

            for (GridPoint next : neighbors(cur)) {
                if (!seen[next.row][next.col] && grid[next.row][next.col] != WALL) {
                    seen[next.row][next.col] = true;
                    parent[next.row][next.col] = cur;
                    queue.offer(next);
                }
            }
        }

        if (result.found) result.path = buildPath(parent);
        return result;
    }

    private SearchResult runDfs() {
        SearchResult result = new SearchResult();
        boolean[][] seen = new boolean[ROWS][COLS];
        GridPoint[][] parent = new GridPoint[ROWS][COLS];
        ArrayDeque<GridPoint> stack = new ArrayDeque<>();

        stack.push(start);
        seen[start.row][start.col] = true;

        while (!stack.isEmpty()) {
            GridPoint cur = stack.pop();
            if (!cur.equals(start)) result.visitedOrder.add(cur);
            if (cur.equals(end)) {
                result.found = true;
                break;
            }

            List<GridPoint> ns = neighbors(cur);
            Collections.reverse(ns);
            for (GridPoint next : ns) {
                if (!seen[next.row][next.col] && grid[next.row][next.col] != WALL) {
                    seen[next.row][next.col] = true;
                    parent[next.row][next.col] = cur;
                    stack.push(next);
                }
            }
        }

        if (result.found) result.path = buildPath(parent);
        return result;
    }

    private SearchResult runDijkstra() {
        SearchResult result = new SearchResult();
        int[][] dist = new int[ROWS][COLS];
        boolean[][] done = new boolean[ROWS][COLS];
        GridPoint[][] parent = new GridPoint[ROWS][COLS];
        for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingInt(s -> s.cost));
        dist[start.row][start.col] = 0;
        pq.offer(new State(start, 0));

        while (!pq.isEmpty()) {
            State state = pq.poll();
            GridPoint cur = state.point;
            if (done[cur.row][cur.col]) continue;
            done[cur.row][cur.col] = true;
            if (!cur.equals(start)) result.visitedOrder.add(cur);

            if (cur.equals(end)) {
                result.found = true;
                break;
            }

            for (GridPoint next : neighbors(cur)) {
                if (grid[next.row][next.col] == WALL || done[next.row][next.col]) continue;
                int nd = dist[cur.row][cur.col] + moveCost(cur, next);
                if (nd < dist[next.row][next.col]) {
                    dist[next.row][next.col] = nd;
                    parent[next.row][next.col] = cur;
                    pq.offer(new State(next, nd));
                }
            }
        }

        if (result.found) result.path = buildPath(parent);
        return result;
    }

    private SearchResult runAStar() {
        SearchResult result = new SearchResult();
        int[][] g = new int[ROWS][COLS];
        boolean[][] closed = new boolean[ROWS][COLS];
        GridPoint[][] parent = new GridPoint[ROWS][COLS];
        for (int[] row : g) Arrays.fill(row, Integer.MAX_VALUE);

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingInt(s -> s.cost));
        g[start.row][start.col] = 0;
        pq.offer(new State(start, heuristic(start, end)));

        while (!pq.isEmpty()) {
            State state = pq.poll();
            GridPoint cur = state.point;
            if (closed[cur.row][cur.col]) continue;
            closed[cur.row][cur.col] = true;
            if (!cur.equals(start)) result.visitedOrder.add(cur);

            if (cur.equals(end)) {
                result.found = true;
                break;
            }

            for (GridPoint next : neighbors(cur)) {
                if (grid[next.row][next.col] == WALL || closed[next.row][next.col]) continue;
                int ng = g[cur.row][cur.col] + moveCost(cur, next);
                if (ng < g[next.row][next.col]) {
                    g[next.row][next.col] = ng;
                    parent[next.row][next.col] = cur;
                    int f = ng + heuristic(next, end);
                    pq.offer(new State(next, f));
                }
            }
        }

        if (result.found) result.path = buildPath(parent);
        return result;
    }

    private List<GridPoint> buildPath(GridPoint[][] parent) {
        List<GridPoint> path = new ArrayList<>();
        GridPoint cur = end;
        while (cur != null && !cur.equals(start)) {
            path.add(cur);
            cur = parent[cur.row][cur.col];
        }
        Collections.reverse(path);
        return path;
    }

    private List<GridPoint> neighbors(GridPoint p) {
        int[][] dirs4 = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
        int[][] dirs8 = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}, {-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
        int[][] dirs = diagonalCheckBox.isSelected() ? dirs8 : dirs4;
        List<GridPoint> list = new ArrayList<>();
        for (int[] d : dirs) {
            int nr = p.row + d[0];
            int nc = p.col + d[1];
            if (!isInside(nr, nc)) continue;
            if (diagonalCheckBox.isSelected() && Math.abs(d[0]) + Math.abs(d[1]) == 2) {
                int side1 = grid[p.row + d[0]][p.col];
                int side2 = grid[p.row][p.col + d[1]];
                if (side1 == WALL && side2 == WALL) continue;
            }
            list.add(new GridPoint(nr, nc));
        }
        return list;
    }

    private int moveCost(GridPoint a, GridPoint b) {
        return (a.row != b.row && a.col != b.col) ? 14 : 10;
    }

    private int heuristic(GridPoint a, GridPoint b) {
        int dr = Math.abs(a.row - b.row);
        int dc = Math.abs(a.col - b.col);
        if (diagonalCheckBox.isSelected()) {
            int min = Math.min(dr, dc);
            int max = Math.max(dr, dc);
            return 14 * min + 10 * (max - min);
        }
        return 10 * (dr + dc);
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    private void animateResult(SearchResult result, String algorithm) {
        running = true;
        startButton.setText("停止");
        updateStatus("正在运行 " + algorithm + "...", result.visitedOrder.size(), result.path.size(), result.timeMs);

        final int[] index = {0};
        final boolean[] pathPhase = {false};
        final List<GridPoint> visited = result.visitedOrder;
        final List<GridPoint> path = result.path;
        double delay = Math.max(5, 210 - speedSlider.getValue() * 2.0);

        animation = new Timeline(new KeyFrame(Duration.millis(delay), event -> {
            if (!pathPhase[0]) {
                if (index[0] < visited.size()) {
                    GridPoint p = visited.get(index[0]++);
                    if (!p.equals(start) && !p.equals(end) && grid[p.row][p.col] != WALL) {
                        grid[p.row][p.col] = VISITED;
                    }
                    drawGrid();
                    return;
                }
                pathPhase[0] = true;
                index[0] = 0;
            }

            if (index[0] < path.size()) {
                GridPoint p = path.get(index[0]++);
                if (!p.equals(start) && !p.equals(end) && grid[p.row][p.col] != WALL) {
                    grid[p.row][p.col] = PATH;
                }
                drawGrid();
            } else {
                finishAnimation(result, algorithm);
            }
        }));

        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    private void finishAnimation(SearchResult result, String algorithm) {
        stopTimelineOnly();
        String message = result.found
                ? algorithm + " 完成：找到路径。"
                : algorithm + " 完成：没有可达路径。";
        updateStatus(message, result.visitedOrder.size(), result.path.size(), result.timeMs);
    }

    private void stopAnimation(String message) {
        stopTimelineOnly();
        if (message != null) {
            updateStatus(message, 0, 0, 0);
        }
    }

    private void stopTimelineOnly() {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
        running = false;
        if (startButton != null) startButton.setText("开始运行");
    }

    private void updateStatus(String status, int visitedCount, int pathLength, double timeMs) {
        if (statusLabel != null) statusLabel.setText(status);
        if (visitedLabel != null) visitedLabel.setText("访问节点：" + visitedCount);
        if (pathLabel != null) pathLabel.setText("路径长度：" + Math.max(0, pathLength));
        if (timeLabel != null) timeLabel.setText(String.format("计算耗时：%.3f ms", timeMs));
    }

    private void drawGrid() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#f8fafc"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gc.setFill(colorOf(grid[r][c]));
                gc.fillRect(c * cellWidth(), r * cellHeight(), cellWidth(), cellHeight());
                gc.setStroke(Color.web("#cbd5e1"));
                gc.strokeRect(c * cellWidth(), r * cellHeight(), cellWidth(), cellHeight());
            }
        }
    }

    private Color colorOf(int type) {
        switch (type) {
            case WALL:
                return Color.web("#111827");
            case START:
                return Color.web("#22c55e");
            case END:
                return Color.web("#ef4444");
            case VISITED:
                return Color.web("#93c5fd");
            case PATH:
                return Color.web("#fde047");
            case EMPTY:
            default:
                return Color.WHITE;
        }
    }

    private double cellWidth() {
        return canvas.getWidth() / COLS;
    }

    private double cellHeight() {
        return canvas.getHeight() / ROWS;
    }

    private static class GridPoint {
        final int row;
        final int col;

        GridPoint(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof GridPoint)) return false;
            GridPoint other = (GridPoint) obj;
            return row == other.row && col == other.col;
        }

        @Override
        public int hashCode() {
            return row * 1009 + col;
        }
    }

    private static class State {
        final GridPoint point;
        final int cost;

        State(GridPoint point, int cost) {
            this.point = point;
            this.cost = cost;
        }
    }

    private static class SearchResult {
        boolean found = false;
        double timeMs = 0;
        List<GridPoint> visitedOrder = new ArrayList<>();
        List<GridPoint> path = new ArrayList<>();
    }
}
