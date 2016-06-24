package br.jus.trf2.apolo.signer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;

public class DocListGet implements IRestAction {

	@Override
	public void run(JSONObject req, JSONObject resp) throws Exception {
		// Parse request
		String cpf = req.getString("cpf");

		// Setup json array
		JSONArray list = new JSONArray();

		// Get documents from Oracle
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		try {
			conn = Utils.getConnection();
			pstmt = conn.prepareStatement(Utils.getSQL("list"));
			pstmt.setString(1, cpf);
			pstmt.setString(2, cpf);
			pstmt.setString(3, cpf);
			pstmt.setString(4, cpf);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				JSONObject doc = new JSONObject();
				Id id = new Id(cpf, rset.getInt("CODSECAO"),
						rset.getLong("CODDOC"),
						rset.getTimestamp("DATA_HORA_MOVIMENTO"), null, 0);
				doc.put("id", id.toString());
				doc.put("code", rset.getString("PROCESSO"));
				doc.put("descr", rset.getString("MOTIVO"));
				doc.put("kind", rset.getString("ATO"));
				doc.put("origin", "Apolo");
				doc.put("urlHash", "apolo/doc/" + doc.getString("id") + "/hash");
				doc.put("urlView", "apolo/doc/" + doc.getString("id") + "/pdf");
				list.put(doc);
			}
		} finally {
			if (rset != null)
				rset.close();
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		}

		resp.put("list", list);
	}

	@Override
	public String getContext() {
		return "listar documentos do Apolo";
	}

}
