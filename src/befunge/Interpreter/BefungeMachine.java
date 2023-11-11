package befunge.Interpreter;

import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import befunge.Components.*;
import befunge.Components.Pair;
import befunge.Instructions.Instruction;
import befunge.Loader.*;


class BefungeModel {
	ProgramView prg_view;
	OutputView out_view;
	BefungeMachine machine;
	StackView stack_view;
	InputView input_view;
	
	Thread thread;
	
	BefungeModel(PrintWriter pw) {
		machine = new BefungeMachine(20, 40, pw);
	}
	
	public void load(String s) {
		machine.input_port.setScanner(null);
		machine.load(s);
		out_view.reset();
		prg_view.load(machine.instr_mem.getMatrix(), 0, 0);
		stack_view.reset();
	
	}
	
	public void step() {
		try {
			if(machine.input_port.getScanner() == null) 
				machine.input_port.setScanner(input_view.make_scanner());
			
			machine.step();
			Pair P = machine.instr_ptr.value();
			prg_view.load(machine.instr_mem.getMatrix(), P.x, P.y);
			out_view.update();
			stack_view.display(machine.stack.toString());
		} catch (OutOfInput e) {
			machine.running = false;
			machine.input_port.getScanner().close();
			machine.input_port.setScanner(null);
			input_view.clear();
		}
	}
	
	public void run() {
		 machine.running = true;
		 if(thread != null) 
			 return;
		 
		 Thread th=new Thread() {
		      public void run() {
		    	  	while(machine.running == true) {
		    	  		step();
		    	  		prg_view.repaint();           
		    	  		try {
		    	  			Thread.sleep(50);
		    	  		} catch (InterruptedException e) {
		    	  			
		    	  			e.printStackTrace();
		    	  		}
		    	  		run();
		    	  	}
		    	  	thread = null;
		      }
		    };
		    thread = th;
		    th.start();
	}
	
	public void stop() {
		machine.running = false;
	}
}

public class BefungeMachine {
	public enum ModeTypes {STRING, CMD};

	public InstrPointer instr_ptr;
	public InstrMemory instr_mem;
	public BefungeInput input_port;
	public BefungeOutput output_port;
	public BefungeStack stack;
	public ModeTypes mode;
	public boolean running;
	
	public int height;
	public int width;

	public BefungeMachine(int height, int width, PrintWriter pw) {
		instr_ptr = new InstrPointer(height, width);
		instr_mem = new InstrMemory(height, width);
		stack = new BefungeStack();
		input_port = new BefungeInput();
		output_port = new BefungeOutput(pw);
		
		this.height = height;
		this.width = width;
		
		reset();
	}

	public void step() throws OutOfInput {
		if(running == false)
			return;
		
		char c = instr_mem.read(instr_ptr.value());
		
		if(mode == ModeTypes.STRING && c != '\"') {
			stack.push(c);
			instr_ptr.update();
		} else {
			Instruction inst = Instruction.get(c);
			inst.execute(this);
		}
	
	}

	public void step(int n) throws OutOfInput {
		while(n-- > 0)
			step();
	}

	public void run() throws OutOfInput {
		while(running) {
			step();
			//System.out.println("Running: " + running);
		}
	}

	public void reset() {
		instr_ptr.reset();
		stack.clear();
		running = true;
		mode = ModeTypes.CMD;
	}

	public void reset_and_clear() {
		reset();
		instr_mem.clear();
	}
	
	public void load(String s) {
		Matrix M = StringToMatrix.makeMatrix(s, height, width);
		instr_mem.load(M);
		reset();
	}
	
}
