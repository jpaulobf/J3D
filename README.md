# 🚀 J3D - Java Software Renderer

Um motor de renderização 3D minimalista e robusto, construído do zero em **Java puro**, sem o uso de bibliotecas gráficas externas (OpenGL, DirectX ou Vulkan). Toda a matemática de matrizes, projeção de perspectiva, rasterização e iluminação é processada exclusivamente pela **CPU**.

## 🛠️ Funcionalidades
* **Rasterização baseada em Triângulos:** Desenho de geometria complexa via software.
* **Z-Buffering:** Algoritmo de profundidade pixel a pixel para evitar artefatos de sobreposição.
* **Iluminação Dinâmica:** Implementação de Reflexão Lambertiana com atenuação de luz por distância.
* **Controle de FPS Alvo:** Sistema de loop que permite fixar a taxa de quadros (30, 60, 120 FPS) com compensação automática de velocidade.
* **Navegação FPS:** Sistema de câmera livre com Yaw e Pitch controlados pelo mouse.
* **Modos de Visualização:** Suporte a preenchimento sólido e modo **Wireframe** (F2).

## 🎮 Comandos e Controles

| Tecla | Função |
| :--- | :--- |
| **W / S** | Move a câmera para Frente / Trás |
| **A / D** | Deslocamento lateral (Strafe) para Esquerda / Direita |
| **Mouse** | Rotaciona a visão (Olhar ao redor) |
| **Setas Direcionais** | Movem o Ponto de Luz nos eixos X e Z |
| **I / K** | Movem o Ponto de Luz no eixo Y (Cima / Baixo) |
| **F2** | Alterna entre modo Sólido e **Wireframe** |
| **F3** | Alterna a visibilidade do Gizmo da Luz |

## 🚀 Como Executar

1. Certifique-se de ter o **JDK 8+** instalado.
2. Compile o arquivo: `javac SoftwareRenderer.java`
3. Execute: `java SoftwareRenderer`

---
Desenvolvido como um laboratório de computação gráfica puramente via Software.