#pragma once
#include <vector>

struct Node {
    int x, y;
    int g, h;
    Node* parent;
    Node(int _x, int _y) : x(_x), y(_y), g(0), h(0), parent(nullptr) {}
    Node(int _x, int _y, int _g, int _h) : x(_x), y(_y), g(_g), h(_h), parent(nullptr) {}
};

using Grid = std::vector<std::vector<int>>;

std::vector<Node*> AStar(Grid& grid, Node start, Node goal);
std::vector<std::vector<Node*>> CBS(Grid& grid, std::vector<Node> starts, std::vector<Node> goals);
