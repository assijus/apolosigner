DECLARE
  p_Seq number := NULL;
  p_DtHrIncl timestamp := NULL;
BEGIN
  DOCUMENTOARQUIVO_I(?,?,p_Seq,?, '0',?,NULL,NULL,?,?,p_DtHrIncl,NULL,?,'N',NULL,NULL,NULL,NULL,NULL,NULL);
  dbms_output.put_line('seq=' || p_Seq);
END;