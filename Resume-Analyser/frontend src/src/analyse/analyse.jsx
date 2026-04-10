import { useContext, useEffect, useState } from "react"
import Styles from "./analyse.module.css"
import { Heat } from "@alptugidin/react-circular-progress-bar"
import { usercontext } from "../appcontext"
import { useNavigate } from "react-router-dom"
function Analyse() {
    const navigate = useNavigate()
    const [score, setscore] = useState(0)
    const [atsscore, setatsscore] = useState(0)
    const [pros, setpros] = useState([])
    const [cons, setcons] = useState([])
    const [sug, setsug] = useState([])
    const [jobs, setjobs] = useState([])
    const { serviceURL } = useContext(usercontext)
    const [isfetched, setisfetched] = useState(false)
    const grade = score >= 85 ? "Elite" : score >= 65 ? "Advanced" : "Growing"

    const normalizeScore = (value) => {
        const num = Number(value)
        if (Number.isNaN(num)) return 0
        return Math.max(0, Math.min(100, Math.round(num)))
    }
    useEffect(
        () => {
            document.getElementById("animate").style.display = "flex";
            fetch(`${serviceURL}/lastReport`, { credentials: "include" }).then(
                response => {
                    if (response.ok) {
                        return response.json()
                        document.getElementById("animate").style.display = "none";
                    }
                    else {
                        console.log("failed")
                        document.getElementById("animate").style.display = "none";
                    }
                }
            ).then(data => {
                if (data != null) {
                    setscore(normalizeScore(data.score))
                    setatsscore(normalizeScore(data.atsoptimizationscore))
                    setpros(data.pros)
                    setcons(data.cons)
                    setsug(data.suggestions)
                    setjobs(data.jobs)
                    console.log(data.jobs)
                    setisfetched(true)
                    document.getElementById("animate").style.display = "none";
                }
            })
                .catch(error => {
                    console.log(error)
                    document.getElementById("animate").style.display = "none";
                })
        }, []
    )


    return (
        <div className={Styles.container}>
            <div className={Styles.nav}>
                <h1>Resume Analyser</h1>
                <button onClick={() => navigate("/uploaddoc")}>Analyse</button>
            </div>

            <div className={Styles.loadani} id="animate">

                <div className={Styles.loadanimation}>
                    <div className={Styles.capstart}></div>
                    <div className={Styles.loadblock}></div>
                </div>
                <h1>Preparing Report</h1>

            </div>

            {isfetched ? <div className={Styles.doc}>
                <div className={Styles.hero}>
                    <img src="https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1400&q=80" alt="Executive analytics dashboard" />
                    <div className={Styles.overlay}>
                        <h2>Executive AI Report</h2>
                        <p>Premium, data-rich view of your ATS strength and role readiness.</p>
                        <span>Performance Tier: {grade}</span>
                    </div>
                </div>
                <div className={Styles.report}>
                    <div className={Styles.sc1}>
                        <Heat
                            progress={score}
                            range={{ from: 0, to: 100 }}
                            sign={{ value: "%", position: "end" }}
                            showValue={true}
                            revertBackground={true}
                            text={'Overall Score'}
                            sx={{
                                barWidth: 10,
                                bgColor: '#e2e8f0',
                                bgStrokeColor: '#ffffff',
                                shapeColor: '#2563eb',
                                valueSize: 18,
                                textSize: 11,
                                valueFamily: 'Poppins',
                                textFamily: 'Poppins',
                                valueWeight: '700',
                                textWeight: '600',
                                textColor: '#334155',
                                valueColor: '#0f172a',
                                loadingTime: 1000,
                                strokeLinecap: 'round',
                                valueAnimation: true,

                            }}
                        />
                    </div>
                    <div className={Styles.sc2}>
                        <Heat
                            progress={atsscore}
                            range={{ from: 0, to: 100 }}
                            sign={{ value: "%", position: "end" }}
                            showValue={true}
                            revertBackground={true}
                            text={'ATS optimization score'}
                            sx={{
                                barWidth: 10,
                                bgColor: '#e2e8f0',
                                bgStrokeColor: '#ffffff',
                                shapeColor: '#0ea5e9',
                                valueSize: 18,
                                textSize: 9,
                                valueFamily: 'Poppins',
                                textFamily: 'Poppins',
                                valueWeight: '700',
                                textWeight: '600',
                                textColor: '#334155',
                                valueColor: '#0f172a',
                                loadingTime: 1000,
                                strokeLinecap: 'round',
                                valueAnimation: true,

                            }}
                        />
                    </div>

                </div>

                <div className={Styles.rev}>
                    <div className={Styles.pros}>
                        <h2>Strengths </h2>
                        <ul>
                            {pros.map((item, index) => <li key={index}>{item}</li>)}
                        </ul>
                    </div>
                    <div className={Styles.cons}>
                        <h2>Improvements</h2>
                        <ul>
                            {cons.map((item, index) => <li key={index}>{item}</li>)}
                        </ul>
                    </div>
                    <div className={Styles.sug}>
                        <h2>Tips to enhance</h2>
                        <ul>
                            {sug.map((item, index) => <li key={index}>{item}</li>)}
                        </ul>
                    </div>
                    {jobs.length > 0 ?
                        <div className={Styles.jobs}>
                            <h2>Suggested Jobs</h2>
                        {jobs.map((item, index) =>
                            <div className={Styles.jobidiv} key={index}>
                                <h3 className={Styles.jobtitle}>Role : {item.title}</h3>
                                <h4 className={Styles.com}>Company : {item.company?.display_name?.trim() || "Not specified"}</h4>
                                <h4 className={Styles.loc}>Location : {item.location?.display_name?.trim() || "Not specified"}</h4>
                                <h4 className={Styles.cat}>Category : {item.category?.label?.trim() || "Not specified"}</h4>
                                <p className={Styles.jobdes}>{item.description}</p>
                                <a className={Styles.joblink} href={item.redirect_url} target="_blank">Apply now</a>
                            </div>
                            )}
                        </div>
                        : null}
                </div>

            </div> : <h1 className={Styles.errinfo}>Something went wrong , Please try again after some time !!!</h1>}

        </div>
    )
}

export default Analyse