export default function BoundingBoxOverlay({ imageUrl, boundingBoxes = [], alt = 'Road damage photo' }) {
  return (
    <div className="bbox-overlay">
      <img src={imageUrl} alt={alt} className="bbox-overlay__image" />
      {boundingBoxes.map((box, index) => (
        <div
          key={index}
          className="bbox-overlay__box"
          style={{
            left: `${box.x * 100}%`,
            top: `${box.y * 100}%`,
            width: `${box.width * 100}%`,
            height: `${box.height * 100}%`,
          }}
        >
          <span className="bbox-overlay__label">
            {box.label} · {box.confidence.toFixed(1)}%
          </span>
        </div>
      ))}
    </div>
  )
}
