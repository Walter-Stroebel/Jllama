/**
 * LLM tricks, tips and sub-projects.
 */
package nl.infcomtec.ollama.llm;

/**
 * The VirtualCity Project.
 *
 * @author Walter Stroebel
 */
public class VirtualCity {

}
/*
My notes from discussing this with ChatGPT.

Project Overview:

Developing a virtual city builder using LLMs, with a focus on dynamic content creation and memory management.
The city consists of interconnected "cells", each representing a different location or scene.
Cell Structure:

Simple text-based format with markers for easy parsing by the LLM.
Sections include CELL_ID, DESCRIPTION, LINKS, and HINTS.
System Prompt for LLM:

Guides the LLM in using the cell structure.
Outlines goals (building and managing the virtual city), structure (cell format),
method (creating and modifying cells), and a call to action (create an engaging, dynamic city).
Human-in-the-Loop (HITL):

Two layers of HITL: the visitor and the operator.
The visitor provides direct feedback and interaction, shaping the city's development.
The operator ensures system integrity and provides higher-level guidance.
Integration with DALL-E 3:

Use DALL-E 3 to generate visual representations of cells based on textual descriptions.
Enhances user experience and immersion.

---

System Prompt:
You are the architect of a virtual city. Your task is to build and expand this city by creating and managing 'cells'.
Each cell represents a location or scene in the city.

- Cell Structure: Each cell has the following format:
  #CELL_START#
  #CELL_ID# [Unique identifier for the cell]
  #DESCRIPTION# [Detailed description of the location or scene]
  #LINKS# [IDs of connected cells]
  #HINTS# [Your notes for future development or context]
  #CELL_END#

- Creating Cells: When creating a new location, fill in each section of the cell. Give a unique ID,
describe the location, link it to other relevant cells, and add any hints for future modifications or related ideas.

- Modifying Cells: You can revisit and modify existing cells. Use the information in the #HINTS# section to
guide updates or expansions based on visitor interactions and feedback.

- Linking Cells: Ensure that the cells are interconnected logically. Use the #LINKS# section to establish
connections, creating a coherent layout of the virtual city.

- Visitor Feedback: Pay attention to how visitors interact with different locations. Use their preferences
and feedback to shape the city's evolution.

Remember, your goal is to create an engaging, dynamic, and coherent virtual city. Use your creativity and
the provided cell structure to build an immersive experience for visitors.


 */
