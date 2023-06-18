import numpy as np
import matplotlib.pyplot as plt
import cv2
import io
import base64
from PIL import Image
import torch
def main(out_3d, out_2d):
    # return (out_3d)
    # convert tensor to numpy array
    keypoints = out_3d.reshape((16, 3))
    # keypoints = np.array([[-0.19792281, -0.5181105,   4.7680383 ],
    # [-0.28452814 ,-0.54827416,  4.7635055 ],
    # [-0.4848649  ,-0.19186205,  4.590798  ],
    # [-0.50190514 , 0.23122746,  4.7385435 ],
    # [-0.09982771 ,-0.49413118,  4.781094  ],
    # [-0.07541415 ,-0.07957169,  4.77617   ],
    # [-0.15102008 , 0.31271493,  4.8622155 ],
    # [-0.18709749 ,-0.9922056 ,  4.6760116 ],
    # [-0.16085428 ,-1.0390875 ,  4.6054025 ],
    # [-0.18120068 ,-1.1443547 ,  4.5985093 ],
    # [-0.09629107 ,-0.93504965,  4.730185  ],
    # [ 0.1705649  ,-0.8561468 ,  4.758465  ],
    # [ 0.3865357  ,-0.8241857 ,  4.726089  ],
    # [-0.33464205 ,-0.90477705,  4.6665993 ],
    # [-0.5666597  ,-0.90143955,  4.6646285 ],
    # [-0.7648208  ,-0.90725195,  4.5434937 ]]) #yoga


    skeleton = ((0, 1), (1, 2), (2, 3),
                (0, 4), (4, 5), (5, 6),
                (0, 7), (7, 8), (8, 9),
                (7, 10), (10, 11), (11, 12),
                (7, 13), (13, 14), (14, 15))


    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')

    # plot keypoints
    for i in range(16):
        ax.scatter(keypoints[i, 0], keypoints[i, 1], keypoints[i, 2], color='blue')

    # plot skeleton lines
    for skel in skeleton:
        start = keypoints[skel[0]]
        end = keypoints[skel[1]]
        xs = [start[0], end[0]]
        ys = [start[1], end[1]]
        zs = [start[2], end[2]]
        ax.plot(xs, ys, zs, color='red')

    ax.set_xlim([-2, 2])
    ax.set_ylim([-2, 2])
    ax.set_zlim([2, 7])
    # ax.view_init(elev=90, azim=90) #$best yet
    # ax.view_init(elev=-50, azim=-75)
    ax.view_init(elev=-79, azim=-90)
    fig.canvas.draw()

    img = np.fromstring(fig.canvas.tostring_rgb(), dtype=np.uint8, sep='')
    img = img.reshape(fig.canvas.get_width_height()[::-1] + (3,))
    img = cv2.cvtColor(img,cv2.COLOR_RGB2BGR)

    pil_im = Image.fromarray(img)
    buff = io.BytesIO()
    pil_im.save(buff, format="PNG")
    img_str = base64.b64encode(buff.getvalue())

    return ""+str(img_str , 'utf-8')