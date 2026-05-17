#include "utils.h"
#include <algorithm>
#include <climits>
#include <cmath>
#include <iostream>
#include <map>
#include <queue>
#include <set>
#include <tuple>
#include <utility>

namespace {
struct Pos {
    int x, y;
    bool operator<(const Pos& other) const {
        if (x != other.x) return x < other.x;
        return y < other.y;
    }
    bool operator==(const Pos& other) const {
        return x == other.x && y == other.y;
    }
};

struct Edge {
    Pos from, to;
    bool operator<(const Edge& other) const {
        if (from < other.from) return true;
        if (other.from < from) return false;
        return to < other.to;
    }
};

int h(Pos a, Pos b) {
    return std::abs(a.x - b.x) + std::abs(a.y - b.y);
}

bool inside(const Grid& grid, int x, int y) {
    return x >= 0 && x < static_cast<int>(grid.size())
        && y >= 0 && y < static_cast<int>(grid[0].size());
}

bool vertexReserved(const std::vector<std::set<Pos>>& reserved, int t, Pos p) {
    if (t < static_cast<int>(reserved.size()) && reserved[t].count(p)) return true;
    if (!reserved.empty() && t >= static_cast<int>(reserved.size()) && reserved.back().count(p)) return true;
    return false;
}

bool edgeReserved(const std::vector<std::set<Edge>>& reservedEdges, int t, Pos from, Pos to) {
    if (t <= 0 || t >= static_cast<int>(reservedEdges.size())) return false;
    Edge opposite{to, from};
    return reservedEdges[t].count(opposite) > 0;
}

std::vector<Node*> timeAStar(Grid& grid, Node startNode, Node goalNode,
                             const std::vector<std::set<Pos>>& reserved,
                             const std::vector<std::set<Edge>>& reservedEdges) {
    std::vector<Node*> path;
    if (grid.empty() || grid[0].empty()) return path;

    Pos start{startNode.x, startNode.y};
    Pos goal{goalNode.x, goalNode.y};
    if (!inside(grid, start.x, start.y) || !inside(grid, goal.x, goal.y)) return path;
    if (grid[start.x][start.y] == 1 || grid[goal.x][goal.y] == 1) return path;

    int rows = static_cast<int>(grid.size());
    int cols = static_cast<int>(grid[0].size());
    int maxT = rows * cols + static_cast<int>(reserved.size()) + 50;

    using State = std::tuple<int, int, int, int, int>; // f, g, x, y, t
    std::priority_queue<State, std::vector<State>, std::greater<State>> pq;
    std::set<std::tuple<int, int, int>> closed;
    std::map<std::tuple<int, int, int>, std::tuple<int, int, int>> parent;

    pq.push({h(start, goal), 0, start.x, start.y, 0});
    int dirs[5][2] = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}, {0, 0}}; // 最后一项表示等待
    std::tuple<int, int, int> goalState{-1, -1, -1};

    while (!pq.empty()) {
        auto [f, g, x, y, t] = pq.top();
        pq.pop();
        auto key = std::make_tuple(x, y, t);
        if (closed.count(key)) continue;
        closed.insert(key);

        if (x == goal.x && y == goal.y && !vertexReserved(reserved, t, goal)) {
            goalState = key;
            break;
        }
        if (t >= maxT) continue;

        for (auto& d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            int nt = t + 1;
            if (!inside(grid, nx, ny) || grid[nx][ny] == 1) continue;
            Pos from{x, y};
            Pos to{nx, ny};
            if (vertexReserved(reserved, nt, to)) continue;
            if (edgeReserved(reservedEdges, nt, from, to)) continue;
            auto nkey = std::make_tuple(nx, ny, nt);
            if (closed.count(nkey)) continue;
            parent[nkey] = key;
            int ng = g + 1;
            pq.push({ng + h(to, goal), ng, nx, ny, nt});
        }
    }

    if (std::get<0>(goalState) == -1) return path;

    std::vector<std::tuple<int, int, int>> rev;
    auto cur = goalState;
    while (true) {
        rev.push_back(cur);
        int x, y, t;
        std::tie(x, y, t) = cur;
        if (x == start.x && y == start.y && t == 0) break;
        cur = parent[cur];
    }
    std::reverse(rev.begin(), rev.end());

    for (auto& item : rev) {
        int x, y, t;
        std::tie(x, y, t) = item;
        Node* node = new Node(x, y, t, h({x, y}, goal));
        if (!path.empty()) node->parent = path.back();
        path.push_back(node);
    }
    return path;
}
}

std::vector<std::vector<Node*>> CBS(Grid& grid, std::vector<Node> starts, std::vector<Node> goals) {
    std::vector<std::vector<Node*>> paths;
    if (starts.size() != goals.size()) {
        std::cout << "CBS: starts and goals size mismatch" << std::endl;
        return paths;
    }

    // 这里实现的是简化版多智能体规划：按智能体顺序规划，并把已规划路径加入时空预约表。
    // 对课程演示够用；如果要严格论文版 CBS，可在此基础上加入冲突树、约束分裂和重新规划。
    std::vector<std::set<Pos>> reserved;
    std::vector<std::set<Edge>> reservedEdges;

    for (size_t i = 0; i < starts.size(); ++i) {
        auto path = timeAStar(grid, starts[i], goals[i], reserved, reservedEdges);
        if (path.empty()) {
            std::cout << "CBS: agent " << i << " no path found" << std::endl;
            paths.push_back(path);
            continue;
        }

        if (reserved.size() < path.size() + 30) reserved.resize(path.size() + 30);
        if (reservedEdges.size() < path.size() + 30) reservedEdges.resize(path.size() + 30);

        for (size_t t = 0; t < path.size(); ++t) {
            Pos cur{path[t]->x, path[t]->y};
            reserved[t].insert(cur);
            if (t > 0) {
                Pos prev{path[t - 1]->x, path[t - 1]->y};
                reservedEdges[t].insert({prev, cur});
            }
        }

        Pos goal{path.back()->x, path.back()->y};
        for (size_t t = path.size(); t < reserved.size(); ++t) {
            reserved[t].insert(goal);
        }

        paths.push_back(path);
    }

    return paths;
}
