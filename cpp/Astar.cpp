#include "utils.h"
#include <algorithm>
#include <climits>
#include <cmath>
#include <iostream>
#include <queue>
#include <tuple>
#include <utility>

namespace {
int heuristic(int x1, int y1, int x2, int y2) {
    return std::abs(x1 - x2) + std::abs(y1 - y2);
}

bool inside(const Grid& grid, int x, int y) {
    return x >= 0 && x < static_cast<int>(grid.size())
        && y >= 0 && y < static_cast<int>(grid[0].size());
}
}

std::vector<Node*> AStar(Grid& grid, Node start, Node goal) {
    std::vector<Node*> path;
    if (grid.empty() || grid[0].empty()) return path;

    int rows = static_cast<int>(grid.size());
    int cols = static_cast<int>(grid[0].size());
    if (!inside(grid, start.x, start.y) || !inside(grid, goal.x, goal.y)) return path;
    if (grid[start.x][start.y] == 1 || grid[goal.x][goal.y] == 1) return path;

    std::vector<std::vector<int>> dist(rows, std::vector<int>(cols, INT_MAX));
    std::vector<std::vector<std::pair<int, int>>> parent(rows, std::vector<std::pair<int, int>>(cols, {-1, -1}));
    std::vector<std::vector<bool>> closed(rows, std::vector<bool>(cols, false));

    using State = std::tuple<int, int, int, int>; // f, g, x, y
    std::priority_queue<State, std::vector<State>, std::greater<State>> pq;
    dist[start.x][start.y] = 0;
    pq.push({heuristic(start.x, start.y, goal.x, goal.y), 0, start.x, start.y});

    int dirs[4][2] = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
    bool found = false;

    while (!pq.empty()) {
        auto [f, g, x, y] = pq.top();
        pq.pop();
        if (closed[x][y]) continue;
        closed[x][y] = true;

        if (x == goal.x && y == goal.y) {
            found = true;
            break;
        }

        for (auto& d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (!inside(grid, nx, ny) || grid[nx][ny] == 1 || closed[nx][ny]) continue;
            int ng = g + 1;
            if (ng < dist[nx][ny]) {
                dist[nx][ny] = ng;
                parent[nx][ny] = {x, y};
                int nf = ng + heuristic(nx, ny, goal.x, goal.y);
                pq.push({nf, ng, nx, ny});
            }
        }
    }

    if (!found) {
        std::cout << "A*: no path found" << std::endl;
        return path;
    }

    std::vector<std::pair<int, int>> rev;
    for (int x = goal.x, y = goal.y; x != -1 && y != -1; ) {
        rev.push_back({x, y});
        if (x == start.x && y == start.y) break;
        auto p = parent[x][y];
        x = p.first;
        y = p.second;
    }
    std::reverse(rev.begin(), rev.end());

    for (int i = 0; i < static_cast<int>(rev.size()); ++i) {
        auto [x, y] = rev[i];
        Node* node = new Node(x, y, i, heuristic(x, y, goal.x, goal.y));
        if (!path.empty()) node->parent = path.back();
        path.push_back(node);
    }

    return path;
}
