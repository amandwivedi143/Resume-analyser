import { useContext, useEffect, useRef, useState } from "react";
import Styles from "./codingtest.module.css";
import { usercontext } from "../appcontext";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";

function CodingTest() {
    const navigate = useNavigate();
    const { serviceURL } = useContext(usercontext);
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [testData, setTestData] = useState(null);
    const [answers, setAnswers] = useState({});
    const [result, setResult] = useState(null);
    const [timeLeft, setTimeLeft] = useState(0);
    const autoSubmittedRef = useRef(false);
    const [formData, setFormData] = useState({
        role: "",
        experience: "",
        topic: "",
        difficulty: "medium",
        numberOfQuestions: 5
    });

    const updateField = (event) => {
        const { name, value } = event.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
    };

    const generateTest = (event) => {
        event.preventDefault();
        if (!formData.role.trim() || !formData.experience.trim() || !formData.topic.trim()) {
            toast.warn("Please fill role, experience and topic");
            return;
        }
        setIsLoading(true);
        setResult(null);
        autoSubmittedRef.current = false;
        fetch(`${serviceURL}/codingTest/generate`, {
            method: "post",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                role: formData.role.trim(),
                experience: formData.experience.trim(),
                topic: formData.topic.trim(),
                difficulty: formData.difficulty,
                numberOfQuestions: Number(formData.numberOfQuestions)
            })
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Test generation failed");
                }
                return response.json();
            })
            .then((data) => {
                setTestData(data);
                setAnswers({});
                setTimeLeft(Number(data.durationSeconds || 0));
                toast.success("Coding test created");
            })
            .catch(() => toast.error("Unable to generate test now"))
            .finally(() => setIsLoading(false));
    };

    const selectOption = (questionId, optionNumber) => {
        setAnswers((prev) => ({ ...prev, [questionId]: optionNumber }));
    };

    const submitTest = (isAutoSubmit = false) => {
        if (!testData || !testData.testId) {
            toast.warn("Generate test first");
            return;
        }
        setIsSubmitting(true);
        fetch(`${serviceURL}/codingTest/submit`, {
            method: "post",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                testId: testData.testId,
                answers
            })
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Submission failed");
                }
                return response.json();
            })
            .then((data) => {
                setResult(data);
                setTestData(null);
                setAnswers({});
                setTimeLeft(0);
                if (!isAutoSubmit) {
                    toast.success("Test submitted");
                } else {
                    toast.info("Time is over. Test auto-submitted.");
                }
            })
            .catch(() => toast.error("Unable to submit test"))
            .finally(() => setIsSubmitting(false));
    };

    const generateWeakAreaTest = () => {
        if (!result?.retryPayload) {
            return;
        }
        setFormData({
            role: result.retryPayload.role || formData.role,
            experience: result.retryPayload.experience || formData.experience,
            topic: result.retryPayload.topic || formData.topic,
            difficulty: result.retryPayload.difficulty || formData.difficulty,
            numberOfQuestions: result.retryPayload.numberOfQuestions || 5
        });
        setResult(null);
        toast.info("Weak area payload ready. Click Generate Test.");
    };

    useEffect(() => {
        if (!testData || timeLeft <= 0 || isSubmitting) {
            return;
        }
        const timer = setInterval(() => {
            setTimeLeft((prev) => {
                if (prev <= 1) {
                    clearInterval(timer);
                    if (!autoSubmittedRef.current) {
                        autoSubmittedRef.current = true;
                        submitTest(true);
                    }
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [testData, timeLeft, isSubmitting]);

    return (
        <div className={Styles.container}>
            <div className={Styles.nav}>
                <h1>Resume Analyser</h1>
                <button onClick={() => navigate("/")}>Home</button>
            </div>

            <div className={Styles.card}>
                <h2>Grok Coding Test</h2>
                <p>Create a role-based coding MCQ test using role, experience and topic.</p>
                <img className={Styles.heroimg} src="https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1400&q=80" alt="Futuristic coding workstation" />

                <form className={Styles.form} onSubmit={generateTest}>
                    <input name="role" value={formData.role} onChange={updateField} placeholder="Role (Ex: Java Backend Developer)" />
                    <input name="experience" value={formData.experience} onChange={updateField} placeholder="Experience (Ex: 2 years)" />
                    <input name="topic" value={formData.topic} onChange={updateField} placeholder="Topic (Ex: Spring Boot, DSA)" />
                    <select name="difficulty" value={formData.difficulty} onChange={updateField}>
                        <option value="easy">Easy</option>
                        <option value="medium">Medium</option>
                        <option value="hard">Hard</option>
                    </select>
                    <input name="numberOfQuestions" type="number" min="3" max="15" value={formData.numberOfQuestions} onChange={updateField} />
                    <button type="submit" disabled={isLoading}>{isLoading ? "Generating..." : "Generate Test"}</button>
                </form>
            </div>

            {testData && testData.questions?.length ? (
                <div className={Styles.questionCard}>
                    <div className={Styles.testMeta}>
                        <h3>Answer all questions</h3>
                        <div className={Styles.chips}>
                            <span>Difficulty: {testData.difficulty}</span>
                            <span>Timer: {formatTime(timeLeft)}</span>
                        </div>
                    </div>
                    {testData.questions.map((question, index) => (
                        <div key={question.id} className={Styles.questionBlock}>
                            <p>{index + 1}. {question.question}</p>
                            <small className={Styles.focus}>Focus: {question.focusArea || formData.topic}</small>
                            <div className={Styles.options}>
                                {question.options.map((option, i) => {
                                    const optionNumber = i + 1;
                                    return (
                                        <label key={`${question.id}-${optionNumber}`}>
                                            <input
                                                type="radio"
                                                name={question.id}
                                                checked={answers[question.id] === optionNumber}
                                                onChange={() => selectOption(question.id, optionNumber)}
                                            />
                                            {option}
                                        </label>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                    <button className={Styles.submitBtn} onClick={submitTest} disabled={isSubmitting}>
                        {isSubmitting ? "Submitting..." : "Submit Test"}
                    </button>
                </div>
            ) : null}

            {result ? (
                <div className={Styles.resultCard}>
                    <h3>Test Score</h3>
                    <p><span>Score:</span> {result.scorePercentage}%</p>
                    <p><span>Correct:</span> {result.correct} / {result.totalQuestions}</p>
                    <p><span>Attempted:</span> {result.attempted}</p>
                    <p><span>Feedback:</span> {result.feedback}</p>
                    {result.weakAreas?.length ? <p><span>Weak Areas:</span> {result.weakAreas.join(", ")}</p> : null}
                    {result.recommendations?.length ? (
                        <div className={Styles.reco}>
                            <h4>Recommendations</h4>
                            <ul>
                                {result.recommendations.map((item, idx) => <li key={idx}>{item}</li>)}
                            </ul>
                        </div>
                    ) : null}
                    <button className={Styles.submitBtn} onClick={generateWeakAreaTest}>Generate Weak Area Test</button>
                    {result.review?.length ? (
                        <div className={Styles.reviewWrap}>
                            <h4>Detailed Review</h4>
                            {result.review.map((item, idx) => (
                                <details key={item.id || idx} className={Styles.reviewItem}>
                                    <summary>{idx + 1}. {item.question}</summary>
                                    <p>Status: {item.isCorrect ? "Correct" : "Incorrect"}</p>
                                    <p>Your Option: {item.selectedOption || "Not attempted"}</p>
                                    <p>Correct Option: {item.correctOption}</p>
                                    <p>Focus Area: {item.focusArea}</p>
                                    <p>Why: {item.explanation}</p>
                                </details>
                            ))}
                        </div>
                    ) : null}
                </div>
            ) : null}
        </div>
    );
}

export default CodingTest;
