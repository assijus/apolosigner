declare 
	i_codsecao number(2,0);
	i_coddoc number(10,0);
	i_dthrmov date;
  	i_cpf varchar2(14);
  	i_pdf_recuperar number(1,0);
  	
	o_pdf_sha1 varchar2(64) := NULL;
	o_pdf_sha256 varchar2(64) := NULL;
	o_pdf_num_paginas number := NULL;
  	o_dthrultatu date := NULL;
  	
  	o_status varchar2(32767) := NULL;
  	o_error varchar2(32767) := NULL;
  	
	o_pdf blob := NULL;
	
  	v_login varchar2(200) := NULL;
begin
	i_codsecao := ?;
	i_coddoc := ?;
	i_dthrmov := ?;
	i_cpf := ?;
	i_pdf_recuperar := ?;

  	select login into v_login from usuario where numcpf = i_cpf and IndAtivo = 'S';
  	dbms_session_set_context(v_login);
  	
  	select PdfSha1, PdfSha256, NumPagesPdf, DtHrUltAtu, TxtPdf into o_pdf_sha1, o_pdf_sha256, o_pdf_num_paginas, o_dthrultatu, o_pdf from MovimentoTextoPdf mtp where mtp.CodSecao = i_codsecao and mtp.CodDoc = i_coddoc and mtp.DtHrMov = i_dthrmov and (mtp.Status='convertidoempdf' or mtp.status='assinado');

  	if (o_pdf_sha256 = null) then
		o_status := 'Erro';
  		o_error := 'PDF não foi gerado!';
  	else
		o_status := 'OK';
  	end if;
  	
  	if (i_pdf_recuperar = 0) then
  		o_pdf := NULL;
  	end if;
  	
  	? := o_pdf_sha1;
	? := o_pdf_sha256;
	? := o_pdf_num_paginas;
  	? := o_dthrultatu;
	? := o_pdf;
  		
	? := o_status;
	? := o_error;
end;