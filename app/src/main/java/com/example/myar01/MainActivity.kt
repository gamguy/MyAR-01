package com.example.myar01

//import android.hardware.camera2.params.RggbChannelVector.RED
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.google.android.material.bottomsheet.BottomSheetBehavior
//import com.google.android.filament.Box
import android.graphics.Color.RED
import android.graphics.Color.WHITE
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

private const val BOTTOM_SHEET_PEEK_HEIGHT =50f
private const val DOUBLE_TAB_TOLERANCE_MS =1000L

class MainActivity : AppCompatActivity() {
    lateinit var arFragment: ArFragment

    private val models = mutableListOf(
        Model(R.drawable.chair, "Chair", R.raw.chair),
        Model(R.drawable.oven, "Oven", R.raw.oven),
        Model(R.drawable.piano, "Piano", R.raw.piano),
        Model(R.drawable.table, "Table", R.raw.table)
    )

    lateinit var selectedModel: Model

    val viewNodes = mutableListOf<Node>()
    private lateinit var photoSaver: PhotoSaver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = fragment as ArFragment
        SetupBottomSheet()
        setupRecyclerView()
        setupDoubleTapArPlaneListener()
        setupFab()

        photoSaver = PhotoSaver(this)

        getCurrentScence().addOnUpdateListener {
            rotateViewNodesTowardsUser()
        }
    }

    private fun setupFab(){
        fab.setOnClickListener {
            photoSaver.takePhoto(arFragment.arSceneView)
        }
    }

    private fun setupDoubleTapArPlaneListener(){
        var firstTabTime = 0L

        arFragment.setOnTapArPlaneListener { hitResult, _,_ ->
            if (firstTabTime == 0L){
                firstTabTime = System.currentTimeMillis()
            }else if (System.currentTimeMillis() - firstTabTime < DOUBLE_TAB_TOLERANCE_MS){
                firstTabTime = 0L
                loadModel { modelRenderable, viewRenderable ->
                    addNodeToScence(hitResult.createAnchor(), modelRenderable, viewRenderable)
                }
            }else{
                firstTabTime = System.currentTimeMillis()
            }
        }
    }

    private fun setupRecyclerView(){
        rvModels.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false)
        rvModels.adapter = ModelAdapter(models).apply{
            selectModel.observe(this@MainActivity, Observer {
                this@MainActivity.selectedModel = it
                val newTitle ="Models(${it.title})"
                tvModel.text = newTitle
            })
        }
    }
    private fun SetupBottomSheet(){
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight=
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                BOTTOM_SHEET_PEEK_HEIGHT,
                resources.displayMetrics).toInt()
        bottomSheetBehavior.addBottomSheetCallback(object :
        BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheet.bringToFront()
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }
        })
    }

    private fun getCurrentScence() = arFragment.arSceneView.scene

    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = "Delete"
            setBackgroundColor(RED)
            setTextColor(WHITE)
        }
    }

    private fun rotateViewNodesTowardsUser(){
        for (node in viewNodes){
            node.renderable?.let{
                val camPos = getCurrentScence().camera.worldPosition
                val viewNodePos = node.worldPosition
                val dir = Vector3.subtract(camPos, viewNodePos)
                node.worldRotation= Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    private fun addNodeToScence(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ){
        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            setParent(anchorNode)
            getCurrentScence().addChild(anchorNode)
            select()
        }
        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box= modelNode.renderable?.collisionShape as Box
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener {
                getCurrentScence().removeChild(anchorNode)
                viewNodes.remove(this)
            }

        }
        viewNodes.add(viewNode)
        modelNode.setOnTapListener{_, _ ->
            if (!modelNode.isTransforming)
                if (viewNode.renderable== null){
                    viewNode.renderable = viewRenderable
                }else{
                    viewNode.renderable = null
                }
        }
    }

    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit){
        val modelRenderable = ModelRenderable.builder()
            .setSource(this, selectedModel.modelResourceId)
            .build()
        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()
        CompletableFuture.allOf(modelRenderable, viewRenderable)
            .thenAccept{
                callback(modelRenderable.get(), viewRenderable.get())
            }
            .exceptionally {
                Toast.makeText(this, "Error loading model: $it", Toast.LENGTH_LONG).show()
                null
            }
    }
}