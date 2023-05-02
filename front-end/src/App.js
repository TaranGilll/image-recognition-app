import './App.css';
import { useState } from "react";

function App() {
  const [uploadedImage, setUploadedImage ] = useState("");
  const [uploadedImageFileName, setUploadedImageFileName ] = useState("");
  const [isLoaded, setIsLoaded ] = useState(false);
  const [labels, setLabels] = useState([]);


  const handleImage = (e) => {
    setIsLoaded(false);
    setUploadedImage(e.target.files[0]);
    setUploadedImageFileName(e.target.files[0].name);

    const image = new File([e.target.files[0]], e.target.files[0].name);

    fetch(API_GATEWAY_ARN + '/upload', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({fileName: e.target.files[0].name}),
    })
    .then(res => res.json())
    .then(resp => {
      const requestOptions = {
        headers: {"Content-Type": image.type},
        method: 'PUT',
        body: image,
      };

      return fetch(resp.imageURL, requestOptions)
        .then((res) => {
          setIsLoaded(true);
        })
        .catch((error) => {
          console.error('Error uploading file:', error);
        });
    })
    .catch((error) => {
      console.error('Error sending request:', error);
    });
  }

  const handleSubmit = () => {
    fetch(API_GATEWAY_ARN + '/image', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({"fileName": uploadedImageFileName}),
    })
    .then(res => res.json())
    .then(data => {
      setLabels(data.labels);
    });
  }

  console.log(labels);

  return (<>
    <div className="imageContainer">
      <h3>Image Recongition</h3>
      {!isLoaded && <p>Please upload an image!</p>}
      <input id="imageInput" type="file" onChange={handleImage}></input>
      {isLoaded &&
        <div style={{marginTop: "20px"}}>
          <img src={URL.createObjectURL(uploadedImage)} style={{maxWidth: "600px", maxHeight: "600px", border: "2px solid #ccc", margin: "auto"}}></img>
          <div>
            <button onClick={handleSubmit}>Analyze Image</button>
          </div>
        </div>
      }
    </div>
    <div style={{display: "flex", justifyContent: "center", alignItems: "center", marginTop: "20px", flexWrap: "wrap"}}>
      {Object.entries(labels).map(obj => (
        <div className="labelContainer">
          <div><b>Label: </b>{obj[1].label}</div>
          <div><b>Confidence: </b>{obj[1].confidence}%</div>
          {!!obj[1].parentLabels && <div><b>Parent Labels: </b>{Object.entries(obj[1].parentLabels).map(label => (<span>{label[1]} </span>))}</div>}
        </div>
        ))
      }
    </div>
  </>);
}

export default App;
