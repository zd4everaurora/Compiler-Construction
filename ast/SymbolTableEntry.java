package cop5555fa13.ast;

import cop5555fa13.TokenStream.Kind;

public class SymbolTableEntry {
	private Kind aKind;
	
	public SymbolTableEntry(Kind kind){
		this.aKind = kind;
	}

	public Kind getaKind() {
		return aKind;
	}

	public void setaKind(Kind aKind) {
		this.aKind = aKind;
	}
}
