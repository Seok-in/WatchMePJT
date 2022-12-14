import React from "react";
import "./ChangePWD.css";
import { useContext, useState } from "react";
import { FetchUrl } from "../../../store/communication";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import swal from "sweetalert";
import ErrorCode from "../../../Error/ErrorCode";

function ChangePWD() {
  const url = `${useContext(FetchUrl)}/members/auth/reset-pwd`;
  const navigate = useNavigate();
  const [inputs, setInputs] = useState({
    emailKey: "",
    password: "",
    check: "",
    isSame: null,
  });

  const [searchParams, setSearchParams] = useSearchParams();
  // Function handling submit button
  const submitHandler = (event) => {
    event.preventDefault();
    const sendJson = {
      password: inputs.password,
      emailKey: searchParams.get("emailKey"),
    };
    if (inputs.password === inputs.check) {
      fetch(url, {
        method: "POST",
        body: JSON.stringify(sendJson),
        headers: {
          "content-type": "application/json",
        },
      })
        .then((response) => {
          return response.json();
        })
        .then((result) => {
          if (result.message === "RESET PASSWORD FAIL") {
            swal("잘못된 정보입니다");
          } else if (result.code === 200) {
            swal("비빌번호가 성공적으로 변경되었습니다.");
            navigate("/");
            ////////////////////////////성공시 여기입니다.
          } else {
            ErrorCode(result);
          }
        })
        .catch((err) => {
          //console.log("ERRROR");
        });
    } else {
      swal("비밀번호가 서로 다릅니다.");
    }
  };
  const handleChange = (event) => {
    const name = event.target.name;
    const value = event.target.value;
    setInputs((values) => ({ ...values, [name]: value }));
  };

  return (
    <div className="login">
      <div className="login-title">
        <div>비밀번호 변경페이지입니다</div>
      </div>
      <form className="login-inputs" onSubmit={submitHandler}>
        <input
          className="input-box"
          type="password"
          onChange={handleChange}
          name="password"
          placeholder="비밀번호를 입력하세요"
        />
        <input
          className="input-box"
          type="password"
          name="check"
          onChange={handleChange}
          placeholder="비밀번호를 다시 입력하세요"
        />
        <button className="submit-btn">확인</button>
      </form>

      <div className="signup-btns">
        <Link to="/login">
          <div className="email"> 메인 페이지로</div>
        </Link>
      </div>
    </div>
  );
}

export default ChangePWD;
