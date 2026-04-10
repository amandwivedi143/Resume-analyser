import Home from "./home/home.jsx";
import Login from "./login/login.jsx";
import "./App.css"
import { ToastContainer } from "react-toastify";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { useContext } from "react";
import { usercontext } from "./appcontext.jsx";
import Forgotpassword from "./resetpassword/resetpassword.jsx";
import Uploadpage from "./upload/upload.jsx";
import Analyse from "./analyse/analyse.jsx";
import CodingTest from "./codingtest/codingtest.jsx";
import CareerRoadmap from "./careerroadmap/careerroadmap.jsx";
import Styles from "./loadstyle.module.css"

function App() {

  const { isauthenticated } = useContext(usercontext)
  return (isauthenticated ?
    <>
      <ToastContainer theme="colored" stacked autoClose={1800} />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/forgotpassword" element={<Forgotpassword />} />
          <Route path="/uploaddoc" element={<Uploadpage />} />
          <Route path="/analysereport" element={<Analyse />} />
          <Route path="/codingtest" element={<CodingTest />} />
          <Route path="/careerroadmap" element={<CareerRoadmap />} />
        </Routes>
      </BrowserRouter>
    </> : 
    <div className={Styles.loadani} id="animate">
      <div className={Styles.loadanimation}>
        <div className={Styles.capstart}></div>
        <div className={Styles.loadblock}></div>
      </div>
    </div>

  )
}

export default App
