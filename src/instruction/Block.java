package instruction;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import expression.Expression;
import expression.SymbolTerminal;
import expression.Terminal;
import view.Console;

/**
 * Represents a block of pseudocode instructions.
 * 
 * @author  Keshav Saharia
 * 			keshav@techlabeducation.com
 * 
 * @license MIT
 */
public class Block extends Instruction {
	
	// Reference to the symbol table. If this block is created by a parent block, this
	// will reference the symbol table of the parent block.
	private HashMap <String, HashMap <String, Double>> symbol;
	private HashMap <String, HashMap <String, Double>> local;
	private HashMap <String, Block> function;
	private Block parent;
	
	//Constants for the types in the symbol tables
	private final static double NUMBER = 0;
	private final static double LIST = 1;
	
	// The console output view.
	private Console console;
	
	// A list of instructions in the block.
	private ArrayList <Instruction> instructions;
	
	// Parameters for a block that is executed as a function
	private ArrayList <String> parameters;
	
	// Integers for storing the progress through the block and its indent level
	private int currentInstruction = 0;
	private int indentLevel = 0;
	
	/**
	 * Creates the root block.
	 */
	public Block() {
		this(null);
	}
	
	/**
	 * Creates a block within the given parent block.
	 * @param parent
	 */
	public Block(Block parent) {
		instructions = new ArrayList <Instruction> ();
		local = new HashMap <String, HashMap<String, Double>> ();
		
		// If this is the root block
		if (parent == null) {
			symbol = new HashMap <String, HashMap<String, Double>> ();
			function = new HashMap <String, Block> ();
		}
		
		// Otherwise take the symbol table from the root block
		else { 
			symbol = parent.symbol;
			indentLevel = parent.indentLevel + 1;
		}
	}
	
	/**
	 * Sets the console output view of this block to the given console.
	 * 
	 * @param console - the Console object representing the console output JFrame
	 */
	public void setConsole(Console console) {
		this.console = console;
	}
	
	/**
	 * Add an instruction to this block.
	 * @param instruction - an Instruction object
	 */
	public void add(Instruction instruction) {
		instruction.setBlock(this);
		instructions.add(instruction);
	}
	
	/**
	 * Adds a parameter to this block.
	 */
	public void addParameter(String parameter) {
		if (parameters == null)
			parameters = new ArrayList <String> ();
		parameters.add(parameter);
	}
	
	/**
	 * Sets the parameters of this block to their current values before evaluating.
	 */
	public void setParameters(Block block, HashMap <String, Expression> arguments) {
		if (parameters != null) {
			for (String parameter : parameters) {
				if (arguments.containsKey(parameter))
					assignLocal(parameter, arguments.get(parameter).evaluate(block));
			}
		}
	}
	
	/**
	 * Executes this block with the given Graphics object and root execution block.
	 */
	public void execute(Graphics graphics, Block block) {
		// If this block is not yet complete
		if (! isComplete()) {
			// Check if the current instruction should execute.
			if (currentInstruction().shouldExecute(block)) {
				
				// If it should, run its execute function with the graphics and block object passed to this object.
				currentInstruction().execute(graphics, block);
				
				// If the current instruction does not request to be repeated, go to the next instruction.
				if (! currentInstruction().shouldRepeat())
					nextInstruction();
			}
			// If it shouldn't be executed, skip it.
			else nextInstruction();
		}
	}

	/**
	 * Returns true if the block is complete, and false otherwise.
	 * @return whether the block has executed all instructions
	 */
	public boolean isComplete() {
		return currentInstruction >= instructions.size();
	}
	
	/**
	 * Gets the indentation level of this block.
	 * @return block indent level
	 */
	public int getIndentLevel() {
		return indentLevel;
	}

	/**
	 * Returns the Instruction object that is currently being performed.
	 * @return the current Instruction object.
	 */
	public Instruction currentInstruction() {
		return instructions.get(currentInstruction);
	}
	
	/**
	 * Returns the previous Instruction object.
	 */
	public Instruction previousInstruction() {
		if (currentInstruction == 0) return null;
		return instructions.get(currentInstruction - 1);
	};

	/**
	 * Resets the program counter to the first instruction.
	 */
	public void reset() {
		currentInstruction = 0;
	}
	
	/**
	 * Advances the program counter to the next instruction.
	 */
	public void nextInstruction() {
		currentInstruction++;
	}
	
	/**
	 * Returns the number of instructions in this block.
	 * @return the size of the block in terms of instructions
	 */
	public int length() {
		return instructions.size();
	}
	
	public HashMap<String, HashMap<String, Double>> getSymbolTable() {
		return symbol;
	}

	/**
	 * Returns the String representation of this block by concatenating the String
	 * representations of each instruction.
	 */
	public String toString() {
		// Create a StringBuilder object for assembling the String representation of this block.
		StringBuilder builder = new StringBuilder();
		// Append the open block syntax
		builder.append("{\n");

		// Append every instruction's String representation
		for (Instruction instruction : instructions) {
			
			// First append the number of tabs to create this block's indentation level
			for (int j = 0 ; j < indentLevel + 1 ; j++)
				builder.append("\t");
			
			// Then append the instruction
			builder.append(instruction.toString());
			
			// Then append a new line.
			builder.append("\n");
		}

		// Append the closing block syntax indented to this block's level.
		for (int j = 0 ; j < indentLevel ; j++)
			builder.append("\t");
		builder.append("}\n");
		return builder.toString();
		
	}
	
	/**
	 * Returns true if this object is equal to another instruction in the parse tree.
	 */
	public boolean equals(Instruction instruction, Block outerBlock) {
		if (instruction instanceof Block) {
			Block other = (Block) instruction;
			
			// If the block has a different number of instructions
			if (this.instructions.size() != other.instructions.size())
				return false;
			
			// Check that every instruction is equal to the other
			for (int i = 0 ; i < this.instructions.size() ; i++)
				if (! this.instructions.get(i).equals(other.instructions.get(i)))
					return false;
			
			// Return true if all tests passed
			return true;
		}
		return false;
	}
	
	/**
	 * Returns true if there is an assigned value to the given symbol.
	 * @param symbol - the name of the symbol
	 * @return true if the symbol exists, false otherwise
	 */
	public boolean hasSymbol(SymbolTerminal symbol) {
		return this.local.containsKey(symbol.toString()) || this.symbol.containsKey(symbol.toString());
	}
	
	/**
	 * Returns true if there is an assigned value to the given symbol.
	 * @param symbol - the name of the symbol
	 * @return true if the symbol exists, false otherwise
	 */
	public boolean hasSymbol(String symbol) {
		return this.local.containsKey(symbol) || this.symbol.containsKey(symbol);
	}
	
	
	/**
	 * Returns the value assigned to the given symbol name.
	 * @param symbol - the name of the symbol
	 * @return the value assigned to the symbol, or 0 if no value was assigned
	 */
	public double get(SymbolTerminal symbol, Expression exp) {
		int index = 0;
		String getName = "value";
		if(exp != null)
			 index = (int) exp.evaluate(this);
		if(this.symbol.get(symbol.toString()).get("type") == LIST){
			if(index >= this.symbol.get(symbol.toString()).get("length"))
				index = 0;
			getName = "" + index;
		}
			index = 0;
		if (local.containsKey(symbol.toString()))
			return local.get(symbol.toString()).get(getName);
		else if (hasSymbol(symbol)){
			return this.symbol.get(symbol.toString()).get(getName);
		}
			
		else return 0;
	}
	
	/**
	 * Returns the value assigned to the given symbol name.
	 * @param symbol - the name of the symbol
	 * @return the value assigned to the symbol, or 0 if no value was assigned
	 */
	public double get(String symbol, int index) {
		String getName = "value";
		if(this.symbol.get(symbol.toString()).get("type") == LIST){
			if(index >= this.symbol.get(symbol.toString()).get("length"))
				index = 0;
			getName = "" + index;
		}
			index = 0;
		if (local.containsKey(symbol.toString()))
			return local.get(symbol.toString()).get(getName);
		else if (hasSymbol(symbol)){
			return this.symbol.get(symbol.toString()).get(getName);
		}
			
		else return 0;
	}
	
	public String get(String symbol){
		return "Todo";
	}
	
	public void assign(String symbol, Expression value){
		HashMap <String, Double> valueMap = new HashMap <String, Double>();
		valueMap.put("type", NUMBER);
		valueMap.put("value", value.evaluate(this));
		this.symbol.put(symbol, valueMap);
	}
	
	public void assign(String symbol, int value){
		HashMap <String, Double> valueMap = new HashMap <String, Double>();
		valueMap.put("type", NUMBER);
		valueMap.put("value", (double)value);
		this.symbol.put(symbol, valueMap);
	}
	
	public void assign(SymbolTerminal symbol, ArrayList<Expression> value){
		HashMap <String, Double> valueMap = new HashMap <String, Double>();
		if(value.size() == 1){
			valueMap.put("type", NUMBER);
			valueMap.put("value", value.get(0).evaluate(this));
			this.symbol.put(symbol.toString(), valueMap);
		}
		else{
			valueMap.put("type", LIST);
			valueMap.put("length", (double) value.size());
			for(int i = 0; i < value.size(); i++){
				valueMap.put("" + i, value.get(i).evaluate(this));
			}
			this.symbol.put(symbol.toString(), valueMap);
		}
	}
	
	public void assignLocal(String symbol, double value){
		if(local.containsKey(symbol)){
			local.get(symbol).put("value", value);	
		}
		HashMap <String, Double> valueMap = new HashMap <String, Double>();
		valueMap.put("type", NUMBER);
		valueMap.put("value", value);
		local.put(symbol, valueMap);
	}
	
	/**
	 * Defines a function with the given name and block.
	 */
	public void define(SymbolTerminal symbol, Block block) {
		this.function.put(symbol.toString(), block);
	}
	
	/**
	 * Defines a function with the given name and block.
	 */
	public void define(String symbol, Block block) {
		this.function.put(symbol, block);
	}
	
	/**
	 * Returns the function definition for the given symbol.
	 */
	public Block getDefinition(String symbol) {
		return this.function.get(symbol);
	}
	
	/**
	 * Prints the given value to the console.
	 */
	public void print(String output) {
		console.print(output);
	}
	
	/**
	 * Prints the given error message to the console.
	 */
	public void error(String message) {
		console.error(message);
	}
}
