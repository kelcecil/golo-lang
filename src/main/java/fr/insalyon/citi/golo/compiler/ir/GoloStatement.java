package fr.insalyon.citi.golo.compiler.ir;

public abstract class GoloStatement {

  private final PositionInSourceCode positionInSourceCode;

  public GoloStatement(PositionInSourceCode positionInSourceCode) {
    this.positionInSourceCode = positionInSourceCode;
  }

  public PositionInSourceCode getPositionInSourceCode() {
    return positionInSourceCode;
  }

  public abstract void accept(GoloIrVisitor visitor);
}